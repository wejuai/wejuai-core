package com.wejuai.core.service;

import com.endofmaster.rest.exception.BadRequestException;
import com.wejuai.core.repository.mongo.CommentRepository;
import com.wejuai.core.repository.mongo.CommentStarRepository;
import com.wejuai.core.repository.mongo.RemindRepository;
import com.wejuai.core.repository.mongo.SubCommentRepository;
import com.wejuai.core.repository.mongo.SystemMessageRepository;
import com.wejuai.core.repository.mysql.ArticleRepository;
import com.wejuai.core.repository.mysql.RewardDemandRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.core.service.dto.HobbyHotType;
import com.wejuai.dto.request.CommentType;
import com.wejuai.dto.request.SaveCommentRequest;
import com.wejuai.dto.request.SaveRemindsRequest;
import com.wejuai.dto.request.SaveSubCommentRequest;
import com.wejuai.dto.response.CommentInfo;
import com.wejuai.dto.response.RemindInfo;
import com.wejuai.dto.response.Slice;
import com.wejuai.dto.response.SubCommentInfo;
import com.wejuai.dto.response.SystemMessageInfo;
import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mongo.Comment;
import com.wejuai.entity.mongo.Remind;
import com.wejuai.entity.mongo.SubComment;
import com.wejuai.entity.mongo.UserPointType;
import com.wejuai.entity.mysql.App;
import com.wejuai.entity.mysql.Article;
import com.wejuai.entity.mysql.RewardDemand;
import com.wejuai.entity.mysql.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author ZM.Wang
 */
@Service
public class CommentService {

    private final Logger logger = LoggerFactory.getLogger(CommentService.class);

    private final UserRepository userRepository;
    private final RemindRepository remindRepository;
    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final SubCommentRepository subCommentRepository;
    private final CommentStarRepository commentStarRepository;
    private final RewardDemandRepository rewardDemandRepository;
    private final SystemMessageRepository systemMessageRepository;

    private final UserService userService;
    private final OrderService orderService;
    private final HobbyService hobbyService;
    private final AppBaseService appBaseService;
    private final WxMessageService wxMessageService;
    private final MongoBaseService mongoBaseService;

    public CommentService(CommentRepository commentRepository, AppBaseService appBaseService, UserRepository userRepository, SubCommentRepository subCommentRepository, RemindRepository remindRepository, SystemMessageRepository systemMessageRepository, CommentStarRepository commentStarRepository, ArticleRepository articleRepository, RewardDemandRepository rewardDemandRepository, OrderService orderService, WxMessageService wxMessageService, UserService userService, HobbyService hobbyService, MongoBaseService mongoBaseService) {
        this.commentRepository = commentRepository;
        this.appBaseService = appBaseService;
        this.userRepository = userRepository;
        this.subCommentRepository = subCommentRepository;
        this.remindRepository = remindRepository;
        this.systemMessageRepository = systemMessageRepository;
        this.commentStarRepository = commentStarRepository;
        this.articleRepository = articleRepository;
        this.rewardDemandRepository = rewardDemandRepository;
        this.orderService = orderService;
        this.wxMessageService = wxMessageService;
        this.userService = userService;
        this.hobbyService = hobbyService;
        this.mongoBaseService = mongoBaseService;
    }

    /** ?????????????????? */
    @Transactional
    public void createComment(SaveCommentRequest request) {
        User sender = userService.getUser(request.getSender());
        App<?> app = commentHandle(sender, request.getAppType(), request.getAppId());
        Comment comment = new Comment(sender.getId(), app.getUser().getId(), app, request.getText());
        userRepository.save(app.getUser().addMsg());
        commentRepository.save(comment);

        //?????????????????????????????????
        new Thread(() -> wxMessageService.sendReplyMsg(comment)).start();
        //????????????
        new Thread(() -> userService.addUserPoint(UserPointType.COMMENTED, sender.getId(), 1, comment.getId())).start();
        new Thread(() -> hobbyService.addHobbyHot(app.getHobby().getId(), HobbyHotType.COMMENTED)).start();
    }

    /** ?????????????????? */
    @Transactional
    public void createSubComment(SaveSubCommentRequest request) {
        User sender = userService.getUser(request.getSender());
        App<?> app = commentHandle(sender, request.getAppType(), request.getAppId());
        Comment comment = getComment(request.getCommentId());
        comment.setSubCommentNum(comment.getSubCommentNum() + 1);
        if (StringUtils.equals(request.getSender(), app.getUser().getId())) {
            comment.setAuthorCommented(true);
        }
        User commentUser = userService.getUser(comment.getSender());
        userRepository.save(commentUser.addMsg());
        SubComment subComment = subCommentRepository.save(new SubComment(sender.getId(), app.getUser().getId(), comment.getSender(), app, request.getText(), comment));

        //?????????????????????????????????
        new Thread(() -> wxMessageService.sendReplyMsg(subComment)).start();
        //????????????
        new Thread(() -> userService.addUserPoint(UserPointType.COMMENTED, sender.getId(), 1, comment.getId())).start();
        new Thread(() -> hobbyService.addHobbyHot(app.getHobby().getId(), HobbyHotType.COMMENTED)).start();
    }

    /** ???????????? */
    @Transactional
    public void createReminds(SaveRemindsRequest request) {
        if (request.getReceiver() == null || request.getReceiver().size() < 1) {
            return;
        }
        User sender = userService.getUser(request.getSender());
        App<?> app = commentHandle(sender, request.getAppType(), request.getAppId());
        String commentId = null;
        if (StringUtils.isNotBlank(request.getCommentId())) {
            commentId = getComment(request.getCommentId()).getId();
        }
        List<String> receiverIds = request.getReceiver();
        receiverIds.removeIf(receiverId -> userRepository.countById(receiverId) <= 0);
        List<User> receivers = new ArrayList<>();
        List<Remind> reminds = new ArrayList<>();
        for (String receiverId : receiverIds) {
            reminds.add(new Remind(sender.getId(), app.getUser().getId(), receiverId, app, request.getText(), commentId));
            User user = userService.getUser(receiverId);
            receivers.add(user.addMsg());
        }
        userRepository.saveAll(receivers);
        remindRepository.saveAll(reminds);

        new Thread(() -> wxMessageService.batchSendRely(receivers, reminds.get(0))).start();
    }

    /** ?????????????????? */
    @Transactional
    public void removeComment(String commentId, String userId) {
        Comment comment = getComment(commentId);
        if (StringUtils.equals(userId, comment.getAppCreator()) || StringUtils.equals(userId, comment.getSender())) {
            App<?> app = appBaseService.getApp(comment.getAppType(), comment.getAppId());
            if (app != null) {
                appBaseService.saveApp(app.unComment());
                new Thread(() -> hobbyService.subHobbyHot(app.getHobby().getId(), HobbyHotType.COMMENTED)).start();
            }
            //????????????
            new Thread(() -> userService.subUserPoint(userId, comment.getId())).start();
            //????????????mongo??????????????????
            commentRepository.delete(comment);
        } else {
            logger.warn("???????????????commentId: {},send: {}???commentSender: {}???appCreator: {}???sender: {}",
                    comment.getId(), userId, comment.getSender(), comment.getAppCreator(), comment.getSender());
            throw new BadRequestException("??????????????????????????????");
        }
    }

    /** ?????????????????? */
    @Transactional
    public void removeSubComment(String subCommentId, String userId) {
        SubComment subComment = getSubComment(subCommentId);
        if (StringUtils.equals(subComment.getSender(), userId) || StringUtils.equals(subComment.getRecipient(), userId)
                || StringUtils.equals(subComment.getAppCreator(), userId)) {
            App<?> app = appBaseService.getApp(subComment.getAppType(), subComment.getAppId());
            if (app != null) {
                appBaseService.saveApp(app.unComment());
                new Thread(() -> hobbyService.subHobbyHot(app.getHobby().getId(), HobbyHotType.COMMENTED)).start();
            }
            //????????????
            new Thread(() -> userService.subUserPoint(userId, subCommentId)).start();
            subCommentRepository.delete(subComment);
        } else {
            logger.warn("????????????, subComment: {}, send: {},commentSender: {}, appCreator: {}, sender: {}",
                    subComment.getId(), userId, subComment.getRecipient(), subComment.getAppCreator(), subComment.getSender());
            throw new BadRequestException("??????????????????????????????");
        }
    }

    /**
     * ????????????????????????
     *
     * @param appType ????????????
     * @param appId   ??????id
     * @param userId  ??????id
     */
    public Slice<CommentInfo> getComments(AppType appType, String appId, String userId, String watchUserId, long page, long size) {
        logger.debug("????????????????????????: appType:{}???appId:{}???userId:{}???watchUserId:{}", appType, appId, userId, watchUserId);
        Criteria criteria = new Criteria();
        if (appType != null) {
            criteria.and("appType").is(appType);
        }
        if (StringUtils.isNotBlank(appId)) {
            criteria.and("appId").is(appId);
        }
        if (StringUtils.isNotBlank(userId)) {
            criteria.and("sender").is(userId);
        }
        if (appType == null && StringUtils.isNotBlank(watchUserId)) {
            criteria.and("appCreator").is(watchUserId);
        }
        long count = mongoBaseService.getMongoPageCount(criteria, Comment.class);
        List<CommentInfo> commentInfos = mongoBaseService.getList(criteria, page, size, Comment.class, Sort.Direction.DESC, "createdAt")
                .stream().map(comment -> getCommentInfo(comment, watchUserId)).collect(Collectors.toList());
        return new Slice<>(commentInfos, page, size, count);
    }

    /**
     * ????????????????????????
     *
     * @param commentId ????????????id
     * @param userId    ??????id
     */
    public Slice<SubCommentInfo> getSubComments(String commentId, String userId, String watchUserId, long page, long size) {
        if (!commentRepository.existsById(commentId)) {
            throw new BadRequestException("?????????????????????: " + commentId);
        }
        Criteria criteria = new Criteria();
        if (StringUtils.isNotBlank(userId)) {
            criteria.and("sender").is(userId);
        }
        if (StringUtils.isNotBlank(commentId)) {
            criteria.and("commentId").is(commentId);
        }
        if (StringUtils.isNotBlank(watchUserId)) {
            Criteria criteria1 = Criteria.where("appCreator").is(watchUserId);
            Criteria criteria2 = Criteria.where("recipient").is(watchUserId);
            criteria.orOperator(criteria1, criteria2);
        }
        long count = mongoBaseService.getMongoPageCount(criteria, SubComment.class);
        List<SubCommentInfo> subComments = mongoBaseService.getList(criteria, page, size, SubComment.class, Sort.Direction.DESC, "createdAt")
                .stream().map(this::getSubCommentInfo).collect(Collectors.toList());
        return new Slice<>(subComments, page, size, count);
    }

    /**
     * ?????????????????????????????????
     *
     * @param recipient ?????????
     */
    public Page<RemindInfo> getReminds(String recipient, Pageable pageable) {
        logger.debug("???????????????: " + recipient);
        Page<Remind> reminds = remindRepository.findByRecipient(recipient, pageable);
        return reminds.map(this::getRemindInfo);
    }

    public void watch(CommentType type, String userId, String id) {
        User user = userService.getUser(userId);
        boolean watch;
        if (type == CommentType.COMMENT) {
            Comment comment = getComment(id);
            User msgUser = userService.getUser(comment.getAppCreator());
            if (!msgUser.equals(user)) {
                throw new BadRequestException("?????????????????????");
            }
            watch = comment.getWatch();
            commentRepository.save(comment.watch());
        } else if (type == CommentType.SUB) {
            SubComment subComment = getSubComment(id);
            User msgUser = userService.getUser(subComment.getRecipient());
            if (!msgUser.equals(user)) {
                throw new BadRequestException("?????????????????????");
            }
            watch = subComment.getWatch();
            subCommentRepository.save(subComment.watch());
        } else if (type == CommentType.REMIND) {
            Remind remind = getRemind(id);
            User msgUser = userService.getUser(remind.getRecipient());
            if (!msgUser.equals(user)) {
                throw new BadRequestException("???@????????????");
            }
            watch = remind.getWatch();
            remindRepository.save(remind.watch());
        } else {
            throw new BadRequestException("?????????????????????:" + type);
        }
        if (!watch) {
            userRepository.save(user.watchMsg());
        }
    }

    public Page<SystemMessageInfo> getSystemMessages(String userId, Pageable pageable) {
        return systemMessageRepository.findByUserId(userId, pageable).map(SystemMessageInfo::new);
    }

    private App<?> commentHandle(User sender, AppType appType, String appId) {
        App<?> app;
        if (appType == AppType.ARTICLE) {
            Article article = articleRepository.findById(appId).orElseThrow(() -> new BadRequestException("???????????????: " + appId));
            if (article.getIntegral() > 0 && !orderService.getUnlock(sender.getId(), article) && !article.getUser().equals(sender)) {
                throw new BadRequestException("??????????????????????????????????????????");
            }
            app = articleRepository.save(article.comment());
        } else if (appType == AppType.REWARD_DEMAND) {
            RewardDemand rewardDemand = rewardDemandRepository.findById(appId).orElseThrow(() -> new BadRequestException("???????????????: " + appId));
            app = rewardDemandRepository.save(rewardDemand.comment());
        } else {
            throw new BadRequestException("?????????????????????: " + appType);
        }
        return app;
    }

    private CommentInfo getCommentInfo(Comment comment, String watchUserId) {
        User appCreator = userService.getUser(comment.getAppCreator());  //???????????????
        User sender = userService.getUser(comment.getSender());          //?????????
        boolean star = commentStarRepository.existsByCommentIdAndUserId(comment.getId(), watchUserId);
        Pageable subPageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");
        Page<SubCommentInfo> subCommentInfos = subCommentRepository.findByCommentId(comment.getId(), subPageable).map(subComment -> {
            User subSender = userService.getUser(subComment.getSender());
            return new SubCommentInfo(subComment, subSender);
        });
        return new CommentInfo(comment, appCreator, sender).setSubComments(new Slice<>(subCommentInfos)).setStar(star);
    }

    private SubCommentInfo getSubCommentInfo(SubComment subComment) {
        User sender = userService.getUser(subComment.getSender());          //?????????
        return new SubCommentInfo(subComment, sender);
    }

    private RemindInfo getRemindInfo(Remind remind) {
        User sender = userService.getUser(remind.getSender());
        User recipient = userService.getUser(remind.getRecipient());
        return new RemindInfo(remind, sender, recipient);
    }

    private Comment getComment(String id) {
        Optional<Comment> commentOptional = commentRepository.findById(id);
        if (commentOptional.isEmpty()) {
            logger.warn("???????????????: " + id);
            throw new BadRequestException("???????????????");
        }
        return commentOptional.get();
    }

    private SubComment getSubComment(String id) {
        Optional<SubComment> subCommentOptional = subCommentRepository.findById(id);
        if (subCommentOptional.isEmpty()) {
            logger.warn("??????????????????: " + id);
            throw new BadRequestException("??????????????????");
        }
        return subCommentOptional.get();
    }

    private Remind getRemind(String id) {
        Optional<Remind> remindOptional = remindRepository.findById(id);
        if (remindOptional.isEmpty()) {
            throw new BadRequestException("???????????????");
        }
        return remindOptional.get();
    }

}
