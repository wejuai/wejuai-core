package com.wejuai.core.service;

import com.endofmaster.rest.exception.BadRequestException;
import com.endofmaster.rest.exception.ForbiddenException;
import com.wejuai.core.repository.mongo.CollectionRepository;
import com.wejuai.core.repository.mongo.StarRepository;
import com.wejuai.core.repository.mysql.ArticleDraftRepository;
import com.wejuai.core.repository.mysql.ArticleRepository;
import com.wejuai.core.repository.mysql.AttentionRepository;
import com.wejuai.core.repository.mysql.HobbyRepository;
import com.wejuai.core.repository.mysql.ImageRepository;
import com.wejuai.core.repository.mysql.OrdersRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.core.service.dto.HobbyHotType;
import com.wejuai.dto.request.AppAddTextRequest;
import com.wejuai.dto.request.ArticleRevokeRequest;
import com.wejuai.dto.request.SaveArticleDraftRequest;
import com.wejuai.dto.request.SaveArticleRequest;
import com.wejuai.dto.response.ArticleInfo;
import com.wejuai.dto.response.ArticleListInfo;
import com.wejuai.dto.response.IdBaseResponse;
import com.wejuai.dto.response.ShareArticleInfo;
import com.wejuai.dto.response.Slice;
import com.wejuai.entity.mongo.Collection;
import com.wejuai.entity.mongo.Star;
import com.wejuai.entity.mongo.UserPointType;
import com.wejuai.entity.mysql.Article;
import com.wejuai.entity.mysql.ArticleDraft;
import com.wejuai.entity.mysql.GiveType;
import com.wejuai.entity.mysql.Hobby;
import com.wejuai.entity.mysql.Image;
import com.wejuai.entity.mysql.ImageUploadType;
import com.wejuai.entity.mysql.Orders;
import com.wejuai.entity.mysql.OrdersType;
import com.wejuai.entity.mysql.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.wejuai.entity.mongo.AppType.ARTICLE;
import static com.wejuai.entity.mysql.GiveType.COLLECT;
import static com.wejuai.entity.mysql.GiveType.STAR;

/**
 * Created by ZM.Wang
 */
@Service
public class ArticleService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StarRepository starRepository;
    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final ImageRepository imageRepository;
    private final OrdersRepository ordersRepository;
    private final ArticleRepository articleRepository;
    private final AttentionRepository attentionRepository;
    private final CollectionRepository collectionRepository;
    private final ArticleDraftRepository articleDraftRepository;

    private final UserService userService;
    private final OrderService orderService;
    private final HobbyService hobbyService;
    private final AppBaseService appBaseService;
    private final MessageService messageService;
    private final MongoBaseService mongoBaseService;
    private final StatisticsService statisticsService;

    public ArticleService(StarRepository starRepository, UserRepository userRepository, CollectionRepository collectionRepository, ArticleRepository articleRepository, ImageRepository imageRepository, ArticleDraftRepository articleDraftRepository, HobbyRepository hobbyRepository, OrdersRepository ordersRepository, AttentionRepository attentionRepository, AppBaseService appBaseService, OrderService orderService, HobbyService hobbyService, UserService userService, StatisticsService statisticsService, MessageService messageService, MongoBaseService mongoBaseService) {
        this.starRepository = starRepository;
        this.userRepository = userRepository;
        this.collectionRepository = collectionRepository;
        this.articleRepository = articleRepository;
        this.imageRepository = imageRepository;
        this.articleDraftRepository = articleDraftRepository;
        this.hobbyRepository = hobbyRepository;
        this.ordersRepository = ordersRepository;
        this.attentionRepository = attentionRepository;
        this.appBaseService = appBaseService;
        this.orderService = orderService;
        this.hobbyService = hobbyService;
        this.userService = userService;
        this.statisticsService = statisticsService;
        this.messageService = messageService;
        this.mongoBaseService = mongoBaseService;
    }

    @Transactional
    public IdBaseResponse saveArticle(String id, SaveArticleRequest request) {
        if (StringUtils.isBlank(request.getHobbyId())) {
            throw new BadRequestException("????????????????????????");
        }
        User user = userService.getUser(request.getUserId());
        Article article = new Article(user);
        if (StringUtils.isNotBlank(id) && articleRepository.existsById(id)) {
            article = articleRepository.findById(id).orElseThrow();
        }
        if (!StringUtils.equals(user.getId(), article.getUser().getId())) {
            throw new ForbiddenException("?????????????????????");
        }
        Image cover = null;
        if (StringUtils.isNotBlank(request.getCover())) {
            cover = imageRepository.findById(request.getCover()).orElseThrow(() -> new BadRequestException("??????????????????: " + request.getCover()));
        }
        article = article.update(request.getTitle(), cover, request.getInShort(), request.getText(), request.getIntegral(), request.getEmailText());
        Hobby hobby = hobbyService.getHobby(request.getHobbyId());
        article = articleRepository.save(article.setHobby(hobby));

        //??????????????????
        if (StringUtils.isNotBlank(id)) {
            new Thread(() -> hobbyService.addHobbyHot(request.getHobbyId(), HobbyHotType.CREATED)).start();
        }
        return new IdBaseResponse(article.getId());
    }

    public void addText(String userId, AppAddTextRequest request) {
        Article article = getArticle(request.getId());
        if (!StringUtils.equals(article.getUser().getId(), userId)) {
            throw new BadRequestException("?????????????????????");
        }
        articleRepository.save(article.addText(request.getAddText()));
    }

    public void updateIntegral(String id, String userId, long integral) {
        User user = userService.getUser(userId);
        Article article = getArticle(id);
        if (!article.getUser().equals(user)) {
            throw new ForbiddenException("?????????????????????");
        }
        articleRepository.save(article.setIntegral(integral));
    }

    @Transactional
    public void deleteArticle(String id, String userId, boolean console, String reason) {
        Article article = deleteArticle(id, userId, console);
        if (console) {
            String content = "?????????????????????????????????" + reason;
            messageService.sendSystemMessage(article.getUser(), content);
        }

        new Thread(() -> hobbyService.subHobbyHot(article.getHobby().getId(), HobbyHotType.CREATED)).start();
    }

    @Transactional
    public void revoke(String id, ArticleRevokeRequest request) {
        Article article = deleteArticle(id, request.getUserId(), request.getConsole());
        articleDraftRepository.save(article.publishArticleDraft());
        String content = "????????????????????????????????????????????????" + (request.getContent() == null ? "" : request.getContent());
        if (request.getConsole()) {
            content += "???????????????????????????????????????????????????";
        }
        messageService.sendSystemMessage(article.getUser(), content);

        new Thread(() -> hobbyService.subHobbyHot(article.getHobby().getId(), HobbyHotType.CREATED)).start();
    }

    @Transactional
    public ArticleDraft saveArticleDraft(SaveArticleDraftRequest request) {
        User user = userService.getUser(request.getUserId());
        Hobby hobby = StringUtils.isBlank(request.getHobbyId()) ? null : hobbyRepository.findById(request.getHobbyId()).orElse(null);
        ArticleDraft articleDraft = null;
        if (StringUtils.isNotBlank(request.getId())) {
            Optional<ArticleDraft> articleDraftOptional = articleDraftRepository.findById(request.getId());
            if (articleDraftOptional.isPresent()) {
                articleDraft = articleDraftOptional.get();
                if (!request.getUserId().equals(articleDraft.getUser().getId())) {
                    throw new BadRequestException("??????????????????????????????~");
                }
            } else {
                throw new BadRequestException("???????????????????????????: " + request.getId());
            }
        }
        if (articleDraft == null) {
            articleDraft = new ArticleDraft(user);
        }
        Image cover;
        if (StringUtils.isNotBlank(request.getCover())) {
            cover = imageRepository.findById(request.getCover()).orElseThrow(() -> new BadRequestException("????????????????????????: " + request.getCover()));
            if (cover.getType() != ImageUploadType.ARTICLE_HEAD) {
                throw new BadRequestException("?????????????????????: " + cover.getType());
            }
        } else {
            cover = articleDraft.getCover();
        }

        articleDraft = articleDraftRepository.save(articleDraft.update(hobby, request.getTitle(), cover, request.getInShort(), request.getText(), request.getIntegral(), request.getEmailText()));
        return articleDraft;
    }

    @Transactional
    public void publishArticle(String draftId, String userId) {
        ArticleDraft articleDraft = getArticleDraft(draftId);
        if (!StringUtils.equals(userId, articleDraft.getUser().getId())) {
            throw new BadRequestException("?????????????????????");
        }
        Article article = articleDraft.publishArticle();
        if (article.getHobby() == null) {
            throw new BadRequestException("???????????????????????????????????????");
        }
        articleRepository.save(article);
        articleDraftRepository.delete(articleDraft);

        new Thread(() -> hobbyService.addHobbyHot(article.getHobby().getId(), HobbyHotType.CREATED)).start();
    }

    public void deleteArticleDraft(String id, String userId) {
        ArticleDraft articleDraft = getArticleDraft(id);
        if (!StringUtils.equals(userId, articleDraft.getUser().getId())) {
            throw new BadRequestException("?????????????????????");
        }
        articleDraftRepository.save(articleDraft.setDel(true));
    }

    public ArticleInfo getArticleInfo(String id, String watchUserId) {
        if (StringUtils.isBlank(watchUserId)) {
            return new ArticleInfo((Article) null);
        }
        Article article = getArticle(id);
        boolean unlock = false;
        User watchUser = userService.getUser(watchUserId);
        if (article.getIntegral() == 0 && !article.getDel()) {
            unlock = true;
        }
        //????????????????????????????????????????????????true????????????????????????????????????????????????????????????????????????????????????????????????
        unlock = unlock || orderService.getUnlock(watchUser.getId(), article);
        boolean author = StringUtils.equals(watchUserId, article.getUser().getId());
        boolean star = starRepository.existsByUserIdAndAppTypeAndAppId(watchUserId, ARTICLE, article.getId());
        boolean collect = collectionRepository.existsByUserIdAndAppTypeAndAppId(watchUserId, ARTICLE, article.getId());
        boolean follow = watchUser != null && attentionRepository.existsByAttentionAndFollow(article.getUser(), watchUser);
        return new ArticleInfo(article, unlock, author, star, collect).setFollow(follow);
    }

    @Transactional
    public void buyArticle(String id, String userId) {
        Article article = getArticle(id);
        long integral = article.getIntegral();
        if (integral < 1) {
            return;
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("???????????????: " + userId));
        if (article.getUser().equals(user)) {
            throw new BadRequestException("??????????????????????????????");
        }
        Orders orders = ordersRepository.findByUserAndAppTypeAndAppIdAndNullifyFalse(user, ARTICLE, id);
        if (orders != null) {
            throw new BadRequestException("???" + article.getTitle() + "?????????????????????~");
        }
        if (user.getIntegral() < integral) {
            throw new BadRequestException("??????????????????");
        }
        User author = article.getUser();
        ordersRepository.save(new Orders(OrdersType.ARTICLE, true, article, author));
        userRepository.save(author.addIntegral(integral));
        ordersRepository.save(new Orders(OrdersType.ARTICLE, false, article, user));
        userRepository.save(user.cutIntegral(integral));

        new Thread(() -> userService.addUserPoint(UserPointType.BUY_ARTICLE, userId, integral * 10, null)).start();
        new Thread(() -> statisticsService.addAppOrders(ARTICLE, integral)).start();
    }

    public ArticleDraft getArticleDraft(String id) {
        Optional<ArticleDraft> articleDraftOptional = articleDraftRepository.findById(id);
        if (articleDraftOptional.isEmpty()) {
            throw new BadRequestException("??????????????????: " + id);
        }
        return articleDraftOptional.get();
    }

    /**
     * ??????????????????
     *
     * @param hobby    ???????????????
     * @param userId   ??????id
     * @param pageable ????????????
     * @return ????????????????????????
     */
    public Page<ArticleListInfo> getArticles(String titleStr, String hobby, String userId, String watchUserId, Pageable pageable) {
        logger.debug("??????: {}???????????????id: {}???????????????id: {}", hobby, userId, watchUserId);
        if (StringUtils.isBlank(watchUserId)) {
            return Page.empty();
        }
        Specification<Article> specification = appBaseService.buildSpecification(titleStr, userId, watchUserId, hobby, true);
        //?????????????????????????????????
        specification = specification.and((Specification<Article>) (root, query, cb) -> cb.and(cb.equal(root.get("authorDel"), false)));
        Page<Article> articles = articleRepository.findAll(specification, pageable);
        if (StringUtils.isNotBlank(watchUserId)) {
            User watchUser = userService.getUser(watchUserId);
            return articles.map(article -> {
                boolean star = starRepository.existsByUserIdAndAppTypeAndAppId(watchUserId, ARTICLE, article.getId());
                boolean collect = collectionRepository.existsByUserIdAndAppTypeAndAppId(watchUserId, ARTICLE, article.getId());
                boolean unLock;
                if (article.getIntegral() == 0) {
                    unLock = true;
                } else {
                    unLock = orderService.getUnlock(watchUser.getId(), article);
                }
                return new ArticleListInfo(article, star, collect, unLock);
            });
        } else {
            return articles.map(ArticleListInfo::new);
        }
    }

    public Page<ArticleListInfo> getArticleDrafts(String hobbyId, String userId, Pageable pageable) {
        Specification<ArticleDraft> specification = appBaseService.buildSpecification(null, userId, userId, hobbyId, false);
        Page<ArticleDraft> articleDrafts = articleDraftRepository.findAll(specification, pageable);
        return articleDrafts.map(ArticleListInfo::new);
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param userId   ??????id
     * @param type     ??????
     * @param pageable ????????????
     * @return ????????????
     */
    public Slice<ArticleListInfo> findArticlesByGiveType(String userId, GiveType type, Pageable pageable) {
        List<Article> articles;
        long count;
        if (type == STAR) {
            Page<Star> stars = starRepository.findByUserIdAndAppType(userId, ARTICLE, pageable);
            count = stars.getTotalElements();
            articles = stars.getContent().stream().map(star ->
                    articleRepository.findById(star.getAppId()).orElse(null)
            ).filter(Objects::nonNull).collect(Collectors.toList());
        } else if (type == COLLECT) {
            Page<Collection> collections = collectionRepository.findByUserIdAndAppType(userId, ARTICLE, pageable);
            count = collections.getTotalElements();
            articles = collections.getContent().stream().map(collection ->
                    articleRepository.findById(collection.getAppId()).orElse(null)
            ).filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            throw new BadRequestException("??????give??????????????????");
        }
        List<ArticleListInfo> articleListInfos = articles.stream().map(article -> {
            boolean collection = collectionRepository.existsByUserIdAndAppTypeAndAppId(userId, ARTICLE, article.getId());
            boolean unlock = orderService.getUnlock(userId, article);
            return new ArticleListInfo(article, true, collection, unlock);
        }).collect(Collectors.toList());
        return new Slice<>(articleListInfos, pageable.getPageNumber(), pageable.getPageSize(), count);
    }

    public ShareArticleInfo getShareInfo(String id) {
        Article article = getArticle(id);
        return new ShareArticleInfo(article);
    }

    private Article deleteArticle(String id, String userId, boolean console) {
        Article article = getArticle(id);
        if ((!StringUtils.equals(userId, article.getUser().getId())) && !console) {
            throw new BadRequestException("?????????????????????");
        }
        String hobbyId = article.getHobby().getId();
        if (console) {
            article = articleRepository.save(article.setDel(true));
            orderService.appRefund(article);
        } else {
            article = articleRepository.save(article.setAuthorDel(true));
        }
        Article finalArticle = article;
        new Thread(() -> mongoBaseService.delAllStarAndCollection(finalArticle, 0)).start();

        //??????????????????
        new Thread(() -> hobbyService.subHobbyHot(hobbyId, HobbyHotType.CREATED)).start();
        return article;
    }


    private Article getArticle(String id) {
        Optional<Article> articleOptional = articleRepository.findById(id);
        if (articleOptional.isEmpty()) {
            throw new BadRequestException("???????????????: " + id);
        }
        return articleOptional.get();
    }

}
