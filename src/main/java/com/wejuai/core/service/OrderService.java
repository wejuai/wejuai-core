package com.wejuai.core.service;


import com.endofmaster.commons.util.DateUtil;
import com.endofmaster.rest.exception.BadRequestException;
import com.endofmaster.rest.exception.ForbiddenException;
import com.wejuai.core.repository.mongo.ChargeRepository;
import com.wejuai.core.repository.mongo.IntegralRefreshProblemRepository;
import com.wejuai.core.repository.mongo.ProblemRepository;
import com.wejuai.core.repository.mysql.ArticleRepository;
import com.wejuai.core.repository.mysql.OrderAppealRepository;
import com.wejuai.core.repository.mysql.OrdersRepository;
import com.wejuai.core.repository.mysql.RewardDemandRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.core.repository.mysql.WithdrawalRepository;
import com.wejuai.dto.request.SaveOrderAppealRequest;
import com.wejuai.dto.request.SaveWithdrawalRequest;
import com.wejuai.dto.response.*;
import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mongo.IntegralRefreshProblem;
import com.wejuai.entity.mongo.Problem;
import com.wejuai.entity.mongo.trade.Charge;
import com.wejuai.entity.mongo.trade.TradeStatus;
import com.wejuai.entity.mysql.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final ChargeRepository chargeRepository;
    private final ArticleRepository articleRepository;
    private final ProblemRepository problemRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final OrderAppealRepository orderAppealRepository;
    private final RewardDemandRepository rewardDemandRepository;
    private final IntegralRefreshProblemRepository integralRefreshProblemRepository;

    private final UserService userService;
    private final MongoBaseService mongoBaseService;
    private final StatisticsService statisticsService;
    private final MessageService messageService;

    public OrderService(OrdersRepository ordersRepository, UserRepository userRepository, ArticleRepository articleRepository, RewardDemandRepository rewardDemandRepository, WithdrawalRepository withdrawalRepository, ChargeRepository chargeRepository, OrderAppealRepository orderAppealRepository, ProblemRepository problemRepository, IntegralRefreshProblemRepository integralRefreshProblemRepository, UserService userService, MongoBaseService mongoBaseService, StatisticsService statisticsService, MessageService messageService) {
        this.ordersRepository = ordersRepository;
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.rewardDemandRepository = rewardDemandRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.chargeRepository = chargeRepository;
        this.orderAppealRepository = orderAppealRepository;
        this.problemRepository = problemRepository;
        this.integralRefreshProblemRepository = integralRefreshProblemRepository;
        this.userService = userService;
        this.mongoBaseService = mongoBaseService;
        this.statisticsService = statisticsService;
        this.messageService = messageService;
    }

    boolean getUnlock(String userId, App<?> app) {
        return ordersRepository.countByUser_IdAndAppIdAndAppTypeAndNullifyFalse(userId, app.getId(), app.getAppType()) > 0;
    }

    @Transactional
    public void addReward(User user, RewardDemand rewardDemand, long integral) {
        userRepository.save(user.cutIntegral(integral));
        rewardDemandRepository.save(rewardDemand.addIntegral(integral));
        ordersRepository.save(new Orders(OrdersType.ADD_REWARD, false, rewardDemand, user));
    }

    @Transactional
    public void cancelRewardDemand(List<RewardSubmission> rewardSubmissions, RewardDemand rewardDemand, User user, long userIntegral, long otherUserIntegral) {
        if (userIntegral > 0) {
            userRepository.save(user.addIntegral(userIntegral));
            ordersRepository.save(new Orders(OrdersType.REWARD_DEMAND_RETURN, true, userIntegral, rewardDemand, user));
        }
        if (otherUserIntegral > 0) {
            rewardSubmissions.forEach(rewardSubmission -> {
                User answer = rewardSubmission.getUser();
                userRepository.save(user.addIntegral(otherUserIntegral));
                ordersRepository.save(new Orders(OrdersType.REWARD_DEMAND_COMPENSATE, true, otherUserIntegral, rewardDemand, answer));
            });
        }
    }

    public Page<OrdersInfo> getOrders(String userId, OrdersType type, Boolean income, boolean notZero, Date start, Date end, Pageable pageable) {
        Specification<Orders> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (notZero) {
                predicates.add(cb.greaterThan(root.get("integral"), 0));
            }
            if (StringUtils.isNotBlank(userId)) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (income != null) {
                predicates.add(cb.equal(root.get("income"), income));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ordersRepository.findAll(specification, pageable).map(OrdersInfo::new);
    }

    @Transactional
    public void withdrawalApply(SaveWithdrawalRequest request) {
        User user = userService.getUser(request.getUserId());
        UserIntegralInfo userIntegralInfo = sumUserWithdrawableIntegral(request.getUserId());
        if (userIntegralInfo.getTotal() < request.getIntegral()) {
            throw new BadRequestException("当前可提现积分: " + userIntegralInfo.getTotal());
        }
        Withdrawal withdrawal = new Withdrawal(user, request.getIntegral(), request.getChannelType());
        if (request.getChannelType() == WithdrawalType.ALIPAY) {
            if (StringUtils.isBlank(request.getCardNo()) || StringUtils.isBlank(request.getName())) {
                throw new BadRequestException("未填写支付宝提现信息");
            }
            withdrawal.alipay(request.getName(), request.getCardNo());
        } else if (request.getChannelType() == WithdrawalType.WEIXIN) {
            if (user.getAccounts().getWeixinUser() == null || StringUtils.isBlank(user.getAccounts().getWeixinUser().getAppOpenId())) {
                throw new BadRequestException("还未绑定微信提现信息");
            }

            withdrawal.weixin(user.getAccounts().getWeixinUser().getAppOpenId());
        }
        withdrawalRepository.save(withdrawal);
        userRepository.save(user.cutIntegral(request.getIntegral()));
        Orders orders = new Orders(OrdersType.CASH_WITHDRAWAL, user, false, request.getIntegral(), "提现");
        ordersRepository.save(orders);
    }

    public Slice<ChargeListInfo> getCharges(String userId, TradeStatus status, ChannelQueryType channelType, Date start, Date end, long page, long size) {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotBlank(userId)) {
            criteria.and("userId").is(userId);
        }
        if (start != null && end != null) {
            criteria.and("startedAt").gte(start).lte(end);
        }
        if (status != null) {
            criteria.and("status").is(status);
        }
        if (channelType != null && channelType != ChannelQueryType.ALL) {
            if (channelType == ChannelQueryType.ALIPAY) {
                Criteria criteria1 = new Criteria("channelType").is(ChannelType.ALIPAY_WEB);
                Criteria criteria2 = new Criteria("channelType").is(ChannelType.ALIPAY_WAP);
                criteria.orOperator(criteria1, criteria2);
            } else {
                Criteria criteria1 = new Criteria("channelType").is(ChannelType.WEIXIN_H5);
                Criteria criteria2 = new Criteria("channelType").is(ChannelType.WEIXIN_JSAPI);
                Criteria criteria3 = new Criteria("channelType").is(ChannelType.WEIXIN_PC);
                criteria.orOperator(criteria1, criteria2, criteria3);
            }
        }
        long count = mongoBaseService.getMongoPageCount(criteria, Charge.class);
        List<ChargeListInfo> chargeInfos = mongoBaseService.getList(criteria, page, size, Charge.class, Sort.Direction.DESC, "startedAt")
                .stream().map(ChargeListInfo::new).collect(Collectors.toList());
        return new Slice<>(chargeInfos, page, size, count);
    }

    public Slice<WithdrawalInfo> getWithdrawals(String id, String userId, ApplyStatus status, WithdrawalType channelType, Date start, Date end, Pageable pageable) {
        if (StringUtils.isBlank(id)) {
            Specification<Withdrawal> specification = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (StringUtils.isNotBlank(userId)) {
                    predicates.add(cb.equal(root.get("user").get("id"), userId));
                }
                if (status != null) {
                    predicates.add(cb.equal(root.get("status"), status));
                }
                if (channelType != null) {
                    predicates.add(cb.equal(root.get("channelType"), channelType));
                }
                if (start != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
                }
                if (end != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            Page<WithdrawalInfo> page = withdrawalRepository.findAll(specification, pageable).map(WithdrawalInfo::new);
            return new Slice<>(page);
        } else {
            Withdrawal withdrawal = withdrawalRepository.findById(id).orElse(null);
            List<WithdrawalInfo> withdrawalInfos = withdrawal == null ? Collections.emptyList() : Collections.singletonList(new WithdrawalInfo(withdrawal));
            return new Slice<>(withdrawalInfos, 0, pageable.getPageSize(), 1);
        }
    }

    public void orderAppeal(SaveOrderAppealRequest request) {
        User user;
        switch (request.getType()) {
            case ORDERS:
                Orders orders = ordersRepository.findById(request.getTypeId()).orElseThrow(() -> new BadRequestException("没有该订单: " + request.getTypeId()));
                user = orders.getUser();
                if (!StringUtils.equals(request.getUserId(), user.getId())) {
                    logger.warn("该订单不属于该用户, userId: {}, ordersId: {}", request.getUserId(), request.getTypeId());
                    throw new ForbiddenException("该订单不属于你");
                }
                break;
            case WITHDRAWAL:
                Withdrawal withdrawal = withdrawalRepository.findById(request.getTypeId()).orElseThrow(() -> new BadRequestException("没有该提现: " + request.getTypeId()));
                user = withdrawal.getUser();
                if (!StringUtils.equals(request.getUserId(), user.getId())) {
                    logger.warn("该提现不属于该用户, userId: {}, withdrawalId: {}", request.getUserId(), request.getTypeId());
                    throw new ForbiddenException("该提现不属于你");
                }
                break;
            case RECHARGE:
                Charge charge = chargeRepository.findById(request.getTypeId()).orElseThrow(() -> new BadRequestException("没有该充值: " + request.getTypeId()));
                user = userService.getUser(charge.getUserId());
                if (!StringUtils.equals(request.getUserId(), user.getId())) {
                    logger.warn("该提现不属于该用户, userId: {}, withdrawalId: {}", request.getUserId(), request.getTypeId());
                    throw new ForbiddenException("该提现不属于你");
                }
                break;
            default:
                throw new BadRequestException("没有该订单类型: " + request.getType());
        }
        orderAppealRepository.save(new OrderAppeal(user, request.getType(), request.getTypeId(), request.getQuestion()));
    }

    public Page<OrderAppealInfo> getOrderAppeals(String userId, LocalDate start, LocalDate end, ApplyStatus status, OrdersPageType type, Pageable pageable) {
        Specification<OrderAppeal> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.isNotBlank(userId)) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), DateUtil.getAnyDayStart(start)));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), DateUtil.getAnyDayEnd(end)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return orderAppealRepository.findAll(specification, pageable).map(OrderAppealInfo::new);
    }

    /**
     * 应用退还积分
     *
     * @param app 应用
     */
    @Transactional
    public void appRefund(App<?> app) {
        long sumIntegral = ordersRepository.sumAppOrdersIntegral(app.getAppType(), app.getId());
        if (sumIntegral > app.getUser().getIntegral()) {
            logger.warn("扣除积分出现问题, 用户剩余积分不足扣除");
            problemRepository.save(new Problem("core", "扣除积分出现问题, 用户剩余积分不足扣除, 现存: " + app.getUser().getIntegral() + ", 需扣除: " + sumIntegral));
        } else {
            subIntegral(app.getUser(), sumIntegral, "系统删除内容扣除需返还购买者的积分");
            messageService.sendSystemMessage(app.getUser(), "系统删除内容, 返还已购用户积分已扣除");
        }
        List<Orders> ordersList = ordersRepository.findByAppTypeAndAppIdAndIncomeFalseAndIntegralAfterAndNullifyFalse(app.getAppType(), app.getId(), 0);
        for (Orders orders : ordersList) {
            addIntegral(orders.getUser(), orders.getIntegral(), "系统删除内容返还购买积分");
            ordersRepository.save(orders.setNullify(true));
            messageService.sendSystemMessage(orders.getUser(), "系统删除已购内容, 积分返还到账");
        }
    }

    @Transactional
    public UserIntegralInfo sumUserWithdrawableIntegral(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("没有该用户: " + userId));
        //总收入
        long totalIncome = ordersRepository.sumUserIntegral(true, userId);
        //总支出
        long totalExpend = ordersRepository.sumUserIntegral(false, userId);
        long hasIntegral = user.getIntegral();
        long actualIntegral = totalIncome - totalExpend;
        if (hasIntegral != actualIntegral) {
            integralRefreshProblemRepository.save(new IntegralRefreshProblem(user.getId(), hasIntegral, actualIntegral));
            user = userRepository.save(user.setIntegral(actualIntegral));
            messageService.sendSystemMessage(user, "重新计算积分发现您的积分有错误, 已更新为正确积分");
        }

        Date start = DateUtil.getAnyDayEnd(LocalDate.now().plusDays(-28));
        Date end = DateUtil.getAnyDayStart(LocalDate.now().plusDays(-14));
        //不可即时提现收入
        long earnIncome = ordersRepository.sumUserAddIntegral(user.getId(), start, end, OrdersType.earnIncome());
        long sumUserProcessIntegral = withdrawalRepository.sumUserProcessIntegral(userId);
        return new UserIntegralInfo(user.getIntegral(), earnIncome, sumUserProcessIntegral);
    }

    public Page<ArticleListInfo> getPurchasedList(String userId, String titleStr, Pageable pageable) {
        Specification<Orders> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("nullify"), false));
            predicates.add(cb.equal(root.get("type"), OrdersType.ARTICLE));
            if (StringUtils.isNotBlank(userId)) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            Join<Orders, Article> articleJoin = root.join("article");
            predicates.add(cb.isNotNull(articleJoin));
            predicates.add(cb.equal(articleJoin.get("del"), false));
            if (StringUtils.isNotBlank(titleStr)) {
                predicates.add(cb.like(articleJoin.get("title"), "%" + titleStr + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ordersRepository.findAll(specification, pageable).map(orders -> {
            Article article = articleRepository.findById(orders.getAppId()).orElse(null);
            assert article != null;
            return new ArticleListInfo(article, true);
        });
    }

    @SuppressWarnings("SameParameterValue")
    @Transactional
    void addIntegral(User user, long integral, String reason) {
        userRepository.save(user.addIntegral(integral));
        ordersRepository.save(new Orders(OrdersType.SYSTEM_ADD, user, true, integral, reason));
        statisticsService.addTransferOrders(true, integral);
    }

    @SuppressWarnings("SameParameterValue")
    @Transactional
    void subIntegral(User user, long integral, String reason) {
        userRepository.save(user.cutIntegral(integral));
        ordersRepository.save(new Orders(OrdersType.SYSTEM_SUB, user, false, integral, reason));
        statisticsService.addTransferOrders(false, integral);
    }

}
