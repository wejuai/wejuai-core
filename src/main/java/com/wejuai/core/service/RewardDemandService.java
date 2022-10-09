package com.wejuai.core.service;

import com.endofmaster.rest.exception.BadRequestException;
import com.wejuai.core.repository.mongo.CollectionRepository;
import com.wejuai.core.repository.mongo.StarRepository;
import com.wejuai.core.repository.mysql.ApplyCancelRewardRemandRepository;
import com.wejuai.core.repository.mysql.AttentionRepository;
import com.wejuai.core.repository.mysql.EvaluateRepository;
import com.wejuai.core.repository.mysql.OrdersRepository;
import com.wejuai.core.repository.mysql.RewardDemandRepository;
import com.wejuai.core.repository.mysql.RewardSubmissionDraftRepository;
import com.wejuai.core.repository.mysql.RewardSubmissionRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.core.service.dto.HobbyHotType;
import com.wejuai.dto.request.AppAddTextRequest;
import com.wejuai.dto.request.ApplyCancelRewardDemandRequest;
import com.wejuai.dto.request.ArticleRevokeRequest;
import com.wejuai.dto.request.SaveRewardDemandRequest;
import com.wejuai.dto.request.SaveRewardSubmissionDraftRequest;
import com.wejuai.dto.request.SaveRewardSubmissionRequest;
import com.wejuai.dto.response.EvaluateInfo;
import com.wejuai.dto.response.RewardDemandInfo;
import com.wejuai.dto.response.RewardDemandListInfo;
import com.wejuai.dto.response.RewardSubmissionDraftInfo;
import com.wejuai.dto.response.RewardSubmissionInfo;
import com.wejuai.dto.response.Slice;
import com.wejuai.entity.mongo.Collection;
import com.wejuai.entity.mongo.Star;
import com.wejuai.entity.mysql.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.wejuai.entity.mongo.AppType.REWARD_DEMAND;
import static com.wejuai.entity.mongo.UserPointType.BE_SELECTED_REWARD_DEMAND;
import static com.wejuai.entity.mongo.UserPointType.REWARD_DEMAND_SUCCESSFUL;
import static com.wejuai.entity.mysql.GiveType.COLLECT;
import static com.wejuai.entity.mysql.GiveType.STAR;
import static com.wejuai.entity.mysql.RewardDemandStatus.END;
import static com.wejuai.entity.mysql.RewardDemandStatus.NORMAL;

/**
 * @author ZM.Wang
 */
@Service
public class RewardDemandService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StarRepository starRepository;
    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final EvaluateRepository evaluateRepository;
    private final AttentionRepository attentionRepository;
    private final CollectionRepository collectionRepository;
    private final RewardDemandRepository rewardDemandRepository;
    private final RewardSubmissionRepository rewardSubmissionRepository;
    private final RewardSubmissionDraftRepository rewardSubmissionDraftRepository;
    private final ApplyCancelRewardRemandRepository applyCancelRewardRemandRepository;

    private final UserService userService;
    private final OrderService orderService;
    private final HobbyService hobbyService;
    private final AppBaseService appBaseService;
    private final MessageService messageService;
    private final WxMessageService wxMessageService;
    private final StatisticsService statisticsService;

    public RewardDemandService(RewardDemandRepository rewardDemandRepository, StarRepository starRepository, CollectionRepository collectionRepository, RewardSubmissionRepository rewardSubmissionRepository, UserRepository userRepository, ApplyCancelRewardRemandRepository applyCancelRewardRemandRepository, EvaluateRepository evaluateRepository, OrdersRepository ordersRepository, AttentionRepository attentionRepository, RewardSubmissionDraftRepository rewardSubmissionDraftRepository, AppBaseService appBaseService, OrderService orderService, HobbyService hobbyService, UserService userService, WxMessageService wxMessageService, StatisticsService statisticsService, MessageService messageService) {
        this.rewardDemandRepository = rewardDemandRepository;
        this.starRepository = starRepository;
        this.collectionRepository = collectionRepository;
        this.rewardSubmissionRepository = rewardSubmissionRepository;
        this.userRepository = userRepository;
        this.applyCancelRewardRemandRepository = applyCancelRewardRemandRepository;
        this.evaluateRepository = evaluateRepository;
        this.ordersRepository = ordersRepository;
        this.attentionRepository = attentionRepository;
        this.rewardSubmissionDraftRepository = rewardSubmissionDraftRepository;
        this.appBaseService = appBaseService;
        this.orderService = orderService;
        this.hobbyService = hobbyService;
        this.userService = userService;
        this.wxMessageService = wxMessageService;
        this.statisticsService = statisticsService;
        this.messageService = messageService;
    }

    /** 创建悬赏 */
    @Transactional
    public void saveRewardDemand(SaveRewardDemandRequest request) {
        User user = userService.getUser(request.getUserId());
        userRepository.save(user.cutIntegral(request.getIntegral()));
        RewardDemand rewardDemand = new RewardDemand(user, request.getIntegral());
        rewardDemand = rewardDemand.update(request.getTitle(), request.getInShort(), request.getText());
        if (rewardDemand.getHobby() == null && StringUtils.isBlank(request.getHobbyId())) {
            throw new BadRequestException("创建悬赏爱好必选");
        }
        Hobby hobby = hobbyService.getHobby(request.getHobbyId());
        rewardDemandRepository.save(rewardDemand.setHobby(hobby));
        ordersRepository.save(new Orders(OrdersType.REWARD_DEMAND, false, rewardDemand, user));

        //爱好热度处理
        new Thread(() -> hobbyService.addHobbyHot(request.getHobbyId(), HobbyHotType.CREATED)).start();
    }

    public void addText(String userId, AppAddTextRequest request) {
        RewardDemand rewardDemand = getRewardDemand(request.getId());
        if (!StringUtils.equals(rewardDemand.getUser().getId(), userId)) {
            throw new BadRequestException("该悬赏不属于你");
        }
        rewardDemandRepository.save(rewardDemand.addText(request.getAddText()));
    }

    /** 增加悬赏金 */
    @Transactional
    public void addReward(String id, String userId, long integral) {
        RewardDemand rewardDemand = getRewardDemand(id);
        User user = userService.getUser(userId);
        if (!rewardDemand.getUser().equals(user)) {
            throw new BadRequestException("该悬赏不属于你");
        }
        if (rewardDemand.getStatus() != RewardDemandStatus.NORMAL) {
            throw new BadRequestException("该悬赏当前状态无法添加悬赏金");
        }
        orderService.addReward(user, rewardDemand, integral);
    }

    /** 删除悬赏 */
    @Transactional
    public void deleteRewardDemand(String id, String userId) {
        RewardDemand rewardDemand = getRewardDemand(id);
        if (!StringUtils.equals(userId, rewardDemand.getUser().getId())) {
            throw new BadRequestException("该悬赏不属于你");
        }
        if (rewardDemand.getStatus() == RewardDemandStatus.LOCKING) {
            throw new BadRequestException("该悬赏已被锁定");
        }
        if (rewardDemand.getStatus() == RewardDemandStatus.NORMAL) {
            List<RewardSubmission> rewardSubmissions = rewardSubmissionRepository.findByRewardDemand(rewardDemand);
            long userIntegral = 0;
            long otherUserIntegral;
            if (rewardSubmissions.size() == 0) {
                otherUserIntegral = 0;
                userIntegral = rewardDemand.getIntegral();
            } else {
                otherUserIntegral = rewardDemand.getIntegral() / rewardSubmissions.size();
            }
            User user = userService.getUser(userId);
            orderService.cancelRewardDemand(rewardSubmissions, rewardDemand, user, userIntegral, otherUserIntegral);
        }

        String hobbyId = rewardDemand.getHobby().getId();
        rewardDemandRepository.save(rewardDemand.setDel(true));
        starRepository.deleteAllByAppTypeAndAppId(rewardDemand.getAppType(), rewardDemand.getId());
        collectionRepository.deleteAllByAppTypeAndAppId(rewardDemand.getAppType(), rewardDemand.getId());
        //爱好热度处理
        new Thread(() -> hobbyService.subHobbyHot(hobbyId, HobbyHotType.CREATED)).start();
    }

    /** 查看悬赏详情 */
    public RewardDemandInfo getRewardDemandInfo(String id, String watchUserId) {
        if (StringUtils.isBlank(watchUserId)) {
            return new RewardDemandInfo(null);
        }
        RewardDemand rewardDemand = getRewardDemand(id);
        RewardDemandInfo rewardDemandInfo = new RewardDemandInfo(rewardDemand);
        User watchUser = userService.getUser(watchUserId);
        boolean star = starRepository.existsByUserIdAndAppTypeAndAppId(watchUserId, REWARD_DEMAND, rewardDemand.getId());
        boolean collect = collectionRepository.existsByUserIdAndAppTypeAndAppId(watchUserId, REWARD_DEMAND, rewardDemand.getId());
        boolean follow = watchUser != null && attentionRepository.existsByAttentionAndFollow(rewardDemand.getUser(), watchUser);
        boolean answer = rewardSubmissionRepository.existsByRewardDemandAndUser_Id(rewardDemand, watchUserId);
        return rewardDemandInfo.setFollow(follow).setCollect(collect).setStar(star).setAnswer(answer);
    }

    /** 悬赏回答分页 */
    public Slice<RewardSubmissionInfo> getRewardSubmissions(String id, String watchUserId, boolean self, Pageable pageable) {
        if (StringUtils.isBlank(watchUserId)) {
            return new Slice<>(Page.empty());
        }
        if (StringUtils.isNotBlank(id)) {
            RewardDemand rewardDemand = rewardDemandRepository.findByIdAndDelFalse(id).orElseThrow(() -> new BadRequestException("没有该悬赏: " + id));
            if (rewardDemand.getStatus() == END) {
                RewardSubmission rewardSubmission = rewardSubmissionRepository.findByRewardDemandAndSelectedTrue(rewardDemand);
                Evaluate evaluate = evaluateRepository.findByAppTypeAndRewardSubmission_Id(REWARD_DEMAND, rewardSubmission.getId());
                boolean author = StringUtils.equals(watchUserId, rewardDemand.getUser().getId()) || StringUtils.equals(watchUserId, rewardSubmission.getUser().getId());
                return new Slice<>(Collections.singletonList(new RewardSubmissionInfo(rewardSubmission, author).setEvaluateInfo(new EvaluateInfo(evaluate))),
                        0, pageable.getPageSize(), 1);
            }
        }
        Specification<RewardSubmission> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.isNotBlank(id) && !self) {
                predicates.add(cb.equal(root.get("rewardDemand").get("id"), id));
                predicates.add(cb.equal(root.get("rewardDemand").get("del"), false));
            }
            if (StringUtils.isNotBlank(id) && self) {
                predicates.add(cb.equal(root.get("rewardDemand").get("id"), id));
                predicates.add(cb.equal(root.get("user").get("id"), watchUserId));
            }
            if (StringUtils.isBlank(id) && self) {
                predicates.add(cb.equal(root.get("user").get("id"), watchUserId));
            }
            if (predicates.size() == 0) {
                predicates.add(cb.isEmpty(root.get("user")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<RewardSubmissionInfo> rewardSubmissions = rewardSubmissionRepository.findAll(specification, pageable).map(rewardSubmission -> {
            RewardDemand rewardDemand = rewardSubmission.getRewardDemand();
            boolean author;
            if (rewardDemand.getStatus() == END) {
                author = StringUtils.equals(watchUserId, rewardDemand.getUser().getId()) || StringUtils.equals(watchUserId, rewardSubmission.getUser().getId());
            } else {
                author = StringUtils.equals(watchUserId, rewardSubmission.getUser().getId());
            }
            return new RewardSubmissionInfo(rewardSubmission, author);
        });
        return new Slice<>(rewardSubmissions);
    }

    /**
     * 查询悬赏列表
     *
     * @param hobby       爱好id
     * @param userId      用户id
     * @param watchUserId 查看者userId
     * @param status      状态
     * @param pageable    分页信息
     * @return 悬赏的基础信息列表
     */
    public Page<RewardDemandListInfo> getRewardDemands(String titleStr, String hobby, String userId, String watchUserId, RewardDemandStatus status, Pageable pageable) {
        if (StringUtils.isBlank(watchUserId)) {
            return Page.empty();
        }
        Specification<RewardDemand> specification1 = appBaseService.buildSpecification(titleStr, userId, watchUserId, hobby, true);
        Specification<RewardDemand> specification2 = buildSpecification(status);
        Specification<RewardDemand> specification = specification1.and(specification2);
        Page<RewardDemand> rewardDemands = rewardDemandRepository.findAll(specification, pageable);
        if (StringUtils.isNotBlank(watchUserId)) {
            userService.getUser(watchUserId);
            return rewardDemands.map(rewardDemand -> {
                boolean star = starRepository.existsByUserIdAndAppTypeAndAppId(watchUserId, REWARD_DEMAND, rewardDemand.getId());
                boolean collect = collectionRepository.existsByUserIdAndAppTypeAndAppId(watchUserId, REWARD_DEMAND, rewardDemand.getId());
                return new RewardDemandListInfo(rewardDemand, star, collect);
            });
        } else {
            return rewardDemands.map(RewardDemandListInfo::new);
        }
    }

    /** 提交悬赏内容 */
    @Transactional
    public void saveResult(SaveRewardSubmissionRequest request) {
        User user = userService.getUser(request.getUserId());
        RewardDemand rewardDemand = getRewardDemand(request.getRewardDemandId());
        if (rewardDemand.getStatus() != RewardDemandStatus.NORMAL) {
            throw new BadRequestException("该悬赏已被锁定");
        }
        if (StringUtils.isNotBlank(request.getId())) {
            Optional<RewardSubmission> rewardSubmissionOptional = rewardSubmissionRepository.findById(request.getId());
            if (rewardSubmissionOptional.isEmpty()) {
                logger.error("没有该悬赏记录: " + request.getId());
                throw new BadRequestException("没有该悬赏提交记录");
            } else {
                RewardSubmission rewardSubmission = rewardSubmissionOptional.get();
                if (!rewardSubmission.getUser().equals(user)) {
                    throw new BadRequestException("该记录不属于你");
                }
                rewardSubmissionRepository.save(rewardSubmission.setText(request.getText()).setInShort(request.getInShort()));
                return;
            }
        } else {
            if (rewardSubmissionRepository.existsByRewardDemandAndUser_Id(rewardDemand, user.getId())) {
                throw new BadRequestException("一个悬赏只能提交一次，你可以修改之前提交的内容");
            }
        }
        if (Double.parseDouble(user.getSeller()) < rewardDemand.getScoreLimit()) {
            throw new BadRequestException("该悬赏发布者要求评分达到" + rewardDemand.getScoreLimit() + "才可以提交回答");
        }
        RewardSubmission rewardSubmission = new RewardSubmission(user, rewardDemand).setText(request.getText()).setInShort(request.getInShort());
        rewardSubmissionRepository.save(rewardSubmission);
        rewardDemandRepository.save(rewardDemand.setRewardSubmissionCount(rewardDemand.getRewardSubmissionCount() + 1));
    }

    /** 选定悬赏结果 */
    @Transactional
    public void selectedResult(String id, String userId) {
        User user = userService.getUser(userId);
        RewardSubmission rewardSubmission = getRewardSubmission(id);
        RewardDemand rewardDemand = rewardSubmission.getRewardDemand();
        if (!StringUtils.equals(rewardDemand.getUser().getId(), userId)) {
            throw new BadRequestException("该悬赏发起者不是你");
        }
        if (rewardDemand.getStatus() != RewardDemandStatus.NORMAL) {
            throw new BadRequestException("当前状态下无法选定结果: " + rewardDemand.getStatus());
        }
        User answerer = rewardSubmission.getUser();
        userRepository.save(answerer.addIntegral(rewardDemand.getIntegral()));
        rewardSubmissionRepository.save(rewardSubmission.setSelected(true));
        rewardDemandRepository.save(rewardDemand.setStatus(END));
        ordersRepository.save(new Orders(OrdersType.SELECTED_REWARD, true, rewardDemand.getIntegral(), rewardDemand, answerer));

        //用户贡献点处理
        new Thread(() -> userService.addUserPoint(REWARD_DEMAND_SUCCESSFUL, rewardDemand.getUser().getId(), rewardDemand.getIntegral() * 10, null)).start();
        new Thread(() -> userService.addUserPoint(BE_SELECTED_REWARD_DEMAND, user.getId(), rewardDemand.getIntegral() * 10, null)).start();

        //积分单统计
        new Thread(() -> statisticsService.addAppOrders(REWARD_DEMAND, rewardDemand.getIntegral())).start();

        //微信模版消息通知被选中的人
        new Thread(() -> wxMessageService.sendRewardDemandSelectedResultMsg(rewardSubmission)).start();
    }

    /** 延期悬赏 */
    public void extensionRewardDemand(String id, String userId, boolean console) {
        RewardDemand rewardDemand = getRewardDemand(id);
        if (!StringUtils.equals(rewardDemand.getUser().getId(), userId) && !console) {
            throw new BadRequestException("该悬赏不属于你");
        }
        if (rewardDemand.getExtension() && !console) {
            throw new BadRequestException("该悬赏已经延期过一次了");
        }
        if (rewardDemand.getStatus() != RewardDemandStatus.NORMAL) {
            throw new BadRequestException("当前状态下无法延期: " + rewardDemand.getStatus());
        }
        rewardDemandRepository.save(rewardDemand.extension());
    }

    /** 申请取消（如接近到期日时如果还都是垃圾回答可以申请全额返还积分的取消） */
    @Transactional
    public void applyCancel(ApplyCancelRewardDemandRequest request) {
        User user = userService.getUser(request.getUserId());
        RewardDemand rewardDemand = getRewardDemand(request.getId());
        if (!rewardDemand.getUser().equals(user)) {
            throw new BadRequestException("该悬赏不属于你");
        }
        if (rewardDemand.getStatus() == END) {
            throw new BadRequestException("该悬赏已经结束");
        }
        if (rewardDemand.getStatus() == RewardDemandStatus.LOCKING) {
            throw new BadRequestException("该悬赏已经被锁定，无需二次操作");
        }
        long difference = rewardDemand.getDeadline().toEpochDay() - LocalDate.now().toEpochDay();
        if (difference - 7 > 0) {
            throw new BadRequestException("只有距离截止日期前7天才能申请全是垃圾回答的全额退回申请，该悬赏距离截止日期还有:" + difference + "天");
        }
        if (applyCancelRewardRemandRepository.existsByRewardDemandAndStatus(rewardDemand, ApplyStatus.PASS)) {
            throw new BadRequestException("该悬赏已经返还过积分了");
        }
        rewardDemandRepository.save(rewardDemand.setStatus(RewardDemandStatus.LOCKING));
        applyCancelRewardRemandRepository.save(new ApplyCancelRewardRemand(user, rewardDemand, request.getReason()));
    }

    /**
     * 分页查询用户的收藏列表或者点赞列表
     *
     * @param userId   帐号id
     * @param type     类型
     * @param pageable 分页信息
     * @return 分页文章
     */
    public Page<RewardDemand> getRewardDemandsByGiveType(String userId, GiveType type, Pageable pageable) {
        if (type == STAR) {
            Page<Star> stars = starRepository.findByUserIdAndAppType(userId, REWARD_DEMAND, pageable);
            return stars.map(star -> {
                Optional<RewardDemand> rewardDemandOptional = rewardDemandRepository.findById(star.getAppId());
                return rewardDemandOptional.orElse(null);
            });
        } else if (type == COLLECT) {
            Page<Collection> collections = collectionRepository.findByUserIdAndAppType(userId, REWARD_DEMAND, pageable);
            return collections.map(collection -> {
                Optional<RewardDemand> rewardDemandOptional = rewardDemandRepository.findById(collection.getAppId());
                return rewardDemandOptional.orElse(null);
            });
        } else {
            throw new BadRequestException("其他give类型无法查看");
        }
    }

    /**
     * 撤回悬赏回答
     *
     * @param id      回答id
     * @param request 其他参数
     */
    @Transactional
    public void revoke(String id, ArticleRevokeRequest request) {
        RewardSubmission rewardSubmission = getRewardSubmission(id);
        RewardDemand rewardDemand = rewardSubmission.getRewardDemand();
        if (rewardDemand.getStatus() != NORMAL) {
            throw new BadRequestException("该悬赏进入无法操作状态");
        }
        delRewardSubmission(rewardSubmission, request.getUserId(), request.getConsole());
        String content = "【撤回的悬赏回答已重新加入到草稿箱】" + (request.getContent() == null ? "" : request.getContent());
        if (request.getConsole()) {
            content += "【系统撤回悬赏回答，请重新修改】";
        }
        messageService.sendSystemMessage(rewardSubmission.getUser(), content);
    }

    public Slice<RewardSubmissionDraftInfo> getRewardSubmissionDrafts(String userId, Pageable pageable) {
        if (StringUtils.isBlank(userId) && StringUtils.isNotBlank(userId)) {
            return new Slice<>(Page.empty());
        }
        Page<RewardSubmissionDraftInfo> page = rewardSubmissionDraftRepository.findByUser_Id(userId, pageable).map(RewardSubmissionDraftInfo::new);
        return new Slice<>(page);
    }

    public RewardSubmissionDraftInfo rewardSubmissionDraftDetails(String draftId, String userId) {
        RewardSubmissionDraft rewardSubmissionDraft = rewardSubmissionDraftRepository.findById(draftId).orElseThrow(() -> new BadRequestException("没有该悬赏回答草稿: " + draftId));
        if (!StringUtils.equals(rewardSubmissionDraft.getUser().getId(), userId)) {
            throw new BadRequestException("该草稿不属于你");
        }
        return new RewardSubmissionDraftInfo(rewardSubmissionDraft, true);
    }

    public void saveRewardSubmissionDraft(SaveRewardSubmissionDraftRequest request) {
        RewardSubmissionDraft rewardSubmissionDraft = rewardSubmissionDraftRepository.findById(request.getId()).orElseThrow(() -> new BadRequestException("没有该悬赏回答草稿: " + request.getId()));
        if (!StringUtils.equals(rewardSubmissionDraft.getUser().getId(), request.getUserId())) {
            throw new BadRequestException("该草稿不属于你: " + request.getId());
        }
        rewardSubmissionDraftRepository.save(rewardSubmissionDraft.setInShort(request.getInShort()).setText(request.getText()));
    }

    @Transactional
    public void publishRewardSubmission(String draftId, String userId) {
        RewardSubmissionDraft rewardSubmissionDraft = rewardSubmissionDraftRepository.findById(draftId).orElseThrow(() -> new BadRequestException("没有该悬赏回答草稿: " + draftId));
        if (!StringUtils.equals(rewardSubmissionDraft.getUser().getId(), userId)) {
            throw new BadRequestException("该草稿不属于你");
        }
        if (rewardSubmissionDraft.getRewardDemand().getStatus() != NORMAL) {
            throw new BadRequestException("该悬赏状态并非可提交回答状态");
        }
        rewardSubmissionRepository.save(new RewardSubmission(rewardSubmissionDraft));
        rewardSubmissionDraftRepository.delete(rewardSubmissionDraft);
    }

    @Transactional
    public void delRewardSubmissionDraft(String id, String userId) {
        RewardSubmissionDraft rewardSubmissionDraft = rewardSubmissionDraftRepository.findById(id).orElse(null);
        if (rewardSubmissionDraft != null) {
            if (!StringUtils.equals(rewardSubmissionDraft.getUser().getId(), userId)) {
                throw new BadRequestException("该草稿不属于你");
            }
            rewardSubmissionDraftRepository.delete(rewardSubmissionDraft);
        }
    }

    RewardSubmission getRewardSubmission(String id) {
        Optional<RewardSubmission> rewardSubmissionOptional = rewardSubmissionRepository.findById(id);
        if (rewardSubmissionOptional.isEmpty()) {
            throw new BadRequestException("没有该悬赏提交: " + id);
        }
        return rewardSubmissionOptional.get();
    }

    void delRewardSubmission(RewardSubmission rewardSubmission, String userId, boolean console) {
        if ((!StringUtils.equals(userId, rewardSubmission.getUser().getId())) && !console) {
            logger.error("该悬赏回答不属于你: " + rewardSubmission.getId());
            throw new BadRequestException("该悬赏回答不属于你");
        }
        if (rewardSubmission.getSelected()) {
            throw new BadRequestException("回答已被选定");
        }
        rewardSubmissionDraftRepository.save(new RewardSubmissionDraft(rewardSubmission));
        rewardSubmissionRepository.delete(rewardSubmission);
    }

    private RewardDemand getRewardDemand(String id) {
        Optional<RewardDemand> rewardDemandOptional = rewardDemandRepository.findById(id);
        if (rewardDemandOptional.isEmpty()) {
            throw new BadRequestException("没有该悬赏: " + id);
        }
        return rewardDemandOptional.get();
    }

    private Specification<RewardDemand> buildSpecification(RewardDemandStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
