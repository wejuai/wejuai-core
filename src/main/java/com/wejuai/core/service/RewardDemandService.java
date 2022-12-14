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

    /** ???????????? */
    @Transactional
    public void saveRewardDemand(SaveRewardDemandRequest request) {
        User user = userService.getUser(request.getUserId());
        userRepository.save(user.cutIntegral(request.getIntegral()));
        RewardDemand rewardDemand = new RewardDemand(user, request.getIntegral());
        rewardDemand = rewardDemand.update(request.getTitle(), request.getInShort(), request.getText());
        if (rewardDemand.getHobby() == null && StringUtils.isBlank(request.getHobbyId())) {
            throw new BadRequestException("????????????????????????");
        }
        Hobby hobby = hobbyService.getHobby(request.getHobbyId());
        rewardDemandRepository.save(rewardDemand.setHobby(hobby));
        ordersRepository.save(new Orders(OrdersType.REWARD_DEMAND, false, rewardDemand, user));

        //??????????????????
        new Thread(() -> hobbyService.addHobbyHot(request.getHobbyId(), HobbyHotType.CREATED)).start();
    }

    public void addText(String userId, AppAddTextRequest request) {
        RewardDemand rewardDemand = getRewardDemand(request.getId());
        if (!StringUtils.equals(rewardDemand.getUser().getId(), userId)) {
            throw new BadRequestException("?????????????????????");
        }
        rewardDemandRepository.save(rewardDemand.addText(request.getAddText()));
    }

    /** ??????????????? */
    @Transactional
    public void addReward(String id, String userId, long integral) {
        RewardDemand rewardDemand = getRewardDemand(id);
        User user = userService.getUser(userId);
        if (!rewardDemand.getUser().equals(user)) {
            throw new BadRequestException("?????????????????????");
        }
        if (rewardDemand.getStatus() != RewardDemandStatus.NORMAL) {
            throw new BadRequestException("??????????????????????????????????????????");
        }
        orderService.addReward(user, rewardDemand, integral);
    }

    /** ???????????? */
    @Transactional
    public void deleteRewardDemand(String id, String userId) {
        RewardDemand rewardDemand = getRewardDemand(id);
        if (!StringUtils.equals(userId, rewardDemand.getUser().getId())) {
            throw new BadRequestException("?????????????????????");
        }
        if (rewardDemand.getStatus() == RewardDemandStatus.LOCKING) {
            throw new BadRequestException("?????????????????????");
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
        //??????????????????
        new Thread(() -> hobbyService.subHobbyHot(hobbyId, HobbyHotType.CREATED)).start();
    }

    /** ?????????????????? */
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

    /** ?????????????????? */
    public Slice<RewardSubmissionInfo> getRewardSubmissions(String id, String watchUserId, boolean self, Pageable pageable) {
        if (StringUtils.isBlank(watchUserId)) {
            return new Slice<>(Page.empty());
        }
        if (StringUtils.isNotBlank(id)) {
            RewardDemand rewardDemand = rewardDemandRepository.findByIdAndDelFalse(id).orElseThrow(() -> new BadRequestException("???????????????: " + id));
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
     * ??????????????????
     *
     * @param hobby       ??????id
     * @param userId      ??????id
     * @param watchUserId ?????????userId
     * @param status      ??????
     * @param pageable    ????????????
     * @return ???????????????????????????
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

    /** ?????????????????? */
    @Transactional
    public void saveResult(SaveRewardSubmissionRequest request) {
        User user = userService.getUser(request.getUserId());
        RewardDemand rewardDemand = getRewardDemand(request.getRewardDemandId());
        if (rewardDemand.getStatus() != RewardDemandStatus.NORMAL) {
            throw new BadRequestException("?????????????????????");
        }
        if (StringUtils.isNotBlank(request.getId())) {
            Optional<RewardSubmission> rewardSubmissionOptional = rewardSubmissionRepository.findById(request.getId());
            if (rewardSubmissionOptional.isEmpty()) {
                logger.error("?????????????????????: " + request.getId());
                throw new BadRequestException("???????????????????????????");
            } else {
                RewardSubmission rewardSubmission = rewardSubmissionOptional.get();
                if (!rewardSubmission.getUser().equals(user)) {
                    throw new BadRequestException("?????????????????????");
                }
                rewardSubmissionRepository.save(rewardSubmission.setText(request.getText()).setInShort(request.getInShort()));
                return;
            }
        } else {
            if (rewardSubmissionRepository.existsByRewardDemandAndUser_Id(rewardDemand, user.getId())) {
                throw new BadRequestException("?????????????????????????????????????????????????????????????????????");
            }
        }
        if (Double.parseDouble(user.getSeller()) < rewardDemand.getScoreLimit()) {
            throw new BadRequestException("????????????????????????????????????" + rewardDemand.getScoreLimit() + "?????????????????????");
        }
        RewardSubmission rewardSubmission = new RewardSubmission(user, rewardDemand).setText(request.getText()).setInShort(request.getInShort());
        rewardSubmissionRepository.save(rewardSubmission);
        rewardDemandRepository.save(rewardDemand.setRewardSubmissionCount(rewardDemand.getRewardSubmissionCount() + 1));
    }

    /** ?????????????????? */
    @Transactional
    public void selectedResult(String id, String userId) {
        User user = userService.getUser(userId);
        RewardSubmission rewardSubmission = getRewardSubmission(id);
        RewardDemand rewardDemand = rewardSubmission.getRewardDemand();
        if (!StringUtils.equals(rewardDemand.getUser().getId(), userId)) {
            throw new BadRequestException("???????????????????????????");
        }
        if (rewardDemand.getStatus() != RewardDemandStatus.NORMAL) {
            throw new BadRequestException("?????????????????????????????????: " + rewardDemand.getStatus());
        }
        User answerer = rewardSubmission.getUser();
        userRepository.save(answerer.addIntegral(rewardDemand.getIntegral()));
        rewardSubmissionRepository.save(rewardSubmission.setSelected(true));
        rewardDemandRepository.save(rewardDemand.setStatus(END));
        ordersRepository.save(new Orders(OrdersType.SELECTED_REWARD, true, rewardDemand.getIntegral(), rewardDemand, answerer));

        //?????????????????????
        new Thread(() -> userService.addUserPoint(REWARD_DEMAND_SUCCESSFUL, rewardDemand.getUser().getId(), rewardDemand.getIntegral() * 10, null)).start();
        new Thread(() -> userService.addUserPoint(BE_SELECTED_REWARD_DEMAND, user.getId(), rewardDemand.getIntegral() * 10, null)).start();

        //???????????????
        new Thread(() -> statisticsService.addAppOrders(REWARD_DEMAND, rewardDemand.getIntegral())).start();

        //???????????????????????????????????????
        new Thread(() -> wxMessageService.sendRewardDemandSelectedResultMsg(rewardSubmission)).start();
    }

    /** ???????????? */
    public void extensionRewardDemand(String id, String userId, boolean console) {
        RewardDemand rewardDemand = getRewardDemand(id);
        if (!StringUtils.equals(rewardDemand.getUser().getId(), userId) && !console) {
            throw new BadRequestException("?????????????????????");
        }
        if (rewardDemand.getExtension() && !console) {
            throw new BadRequestException("?????????????????????????????????");
        }
        if (rewardDemand.getStatus() != RewardDemandStatus.NORMAL) {
            throw new BadRequestException("???????????????????????????: " + rewardDemand.getStatus());
        }
        rewardDemandRepository.save(rewardDemand.extension());
    }

    /** ????????????????????????????????????????????????????????????????????????????????????????????????????????? */
    @Transactional
    public void applyCancel(ApplyCancelRewardDemandRequest request) {
        User user = userService.getUser(request.getUserId());
        RewardDemand rewardDemand = getRewardDemand(request.getId());
        if (!rewardDemand.getUser().equals(user)) {
            throw new BadRequestException("?????????????????????");
        }
        if (rewardDemand.getStatus() == END) {
            throw new BadRequestException("?????????????????????");
        }
        if (rewardDemand.getStatus() == RewardDemandStatus.LOCKING) {
            throw new BadRequestException("?????????????????????????????????????????????");
        }
        long difference = rewardDemand.getDeadline().toEpochDay() - LocalDate.now().toEpochDay();
        if (difference - 7 > 0) {
            throw new BadRequestException("???????????????????????????7??????????????????????????????????????????????????????????????????????????????????????????:" + difference + "???");
        }
        if (applyCancelRewardRemandRepository.existsByRewardDemandAndStatus(rewardDemand, ApplyStatus.PASS)) {
            throw new BadRequestException("?????????????????????????????????");
        }
        rewardDemandRepository.save(rewardDemand.setStatus(RewardDemandStatus.LOCKING));
        applyCancelRewardRemandRepository.save(new ApplyCancelRewardRemand(user, rewardDemand, request.getReason()));
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param userId   ??????id
     * @param type     ??????
     * @param pageable ????????????
     * @return ????????????
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
            throw new BadRequestException("??????give??????????????????");
        }
    }

    /**
     * ??????????????????
     *
     * @param id      ??????id
     * @param request ????????????
     */
    @Transactional
    public void revoke(String id, ArticleRevokeRequest request) {
        RewardSubmission rewardSubmission = getRewardSubmission(id);
        RewardDemand rewardDemand = rewardSubmission.getRewardDemand();
        if (rewardDemand.getStatus() != NORMAL) {
            throw new BadRequestException("?????????????????????????????????");
        }
        delRewardSubmission(rewardSubmission, request.getUserId(), request.getConsole());
        String content = "??????????????????????????????????????????????????????" + (request.getContent() == null ? "" : request.getContent());
        if (request.getConsole()) {
            content += "????????????????????????????????????????????????";
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
        RewardSubmissionDraft rewardSubmissionDraft = rewardSubmissionDraftRepository.findById(draftId).orElseThrow(() -> new BadRequestException("???????????????????????????: " + draftId));
        if (!StringUtils.equals(rewardSubmissionDraft.getUser().getId(), userId)) {
            throw new BadRequestException("?????????????????????");
        }
        return new RewardSubmissionDraftInfo(rewardSubmissionDraft, true);
    }

    public void saveRewardSubmissionDraft(SaveRewardSubmissionDraftRequest request) {
        RewardSubmissionDraft rewardSubmissionDraft = rewardSubmissionDraftRepository.findById(request.getId()).orElseThrow(() -> new BadRequestException("???????????????????????????: " + request.getId()));
        if (!StringUtils.equals(rewardSubmissionDraft.getUser().getId(), request.getUserId())) {
            throw new BadRequestException("?????????????????????: " + request.getId());
        }
        rewardSubmissionDraftRepository.save(rewardSubmissionDraft.setInShort(request.getInShort()).setText(request.getText()));
    }

    @Transactional
    public void publishRewardSubmission(String draftId, String userId) {
        RewardSubmissionDraft rewardSubmissionDraft = rewardSubmissionDraftRepository.findById(draftId).orElseThrow(() -> new BadRequestException("???????????????????????????: " + draftId));
        if (!StringUtils.equals(rewardSubmissionDraft.getUser().getId(), userId)) {
            throw new BadRequestException("?????????????????????");
        }
        if (rewardSubmissionDraft.getRewardDemand().getStatus() != NORMAL) {
            throw new BadRequestException("??????????????????????????????????????????");
        }
        rewardSubmissionRepository.save(new RewardSubmission(rewardSubmissionDraft));
        rewardSubmissionDraftRepository.delete(rewardSubmissionDraft);
    }

    @Transactional
    public void delRewardSubmissionDraft(String id, String userId) {
        RewardSubmissionDraft rewardSubmissionDraft = rewardSubmissionDraftRepository.findById(id).orElse(null);
        if (rewardSubmissionDraft != null) {
            if (!StringUtils.equals(rewardSubmissionDraft.getUser().getId(), userId)) {
                throw new BadRequestException("?????????????????????");
            }
            rewardSubmissionDraftRepository.delete(rewardSubmissionDraft);
        }
    }

    RewardSubmission getRewardSubmission(String id) {
        Optional<RewardSubmission> rewardSubmissionOptional = rewardSubmissionRepository.findById(id);
        if (rewardSubmissionOptional.isEmpty()) {
            throw new BadRequestException("?????????????????????: " + id);
        }
        return rewardSubmissionOptional.get();
    }

    void delRewardSubmission(RewardSubmission rewardSubmission, String userId, boolean console) {
        if ((!StringUtils.equals(userId, rewardSubmission.getUser().getId())) && !console) {
            logger.error("???????????????????????????: " + rewardSubmission.getId());
            throw new BadRequestException("???????????????????????????");
        }
        if (rewardSubmission.getSelected()) {
            throw new BadRequestException("??????????????????");
        }
        rewardSubmissionDraftRepository.save(new RewardSubmissionDraft(rewardSubmission));
        rewardSubmissionRepository.delete(rewardSubmission);
    }

    private RewardDemand getRewardDemand(String id) {
        Optional<RewardDemand> rewardDemandOptional = rewardDemandRepository.findById(id);
        if (rewardDemandOptional.isEmpty()) {
            throw new BadRequestException("???????????????: " + id);
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
