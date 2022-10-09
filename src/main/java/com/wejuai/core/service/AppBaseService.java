package com.wejuai.core.service;

import com.endofmaster.rest.exception.BadRequestException;
import com.wejuai.core.repository.mongo.CollectionRepository;
import com.wejuai.core.repository.mongo.StarRepository;
import com.wejuai.core.repository.mysql.ArticleRepository;
import com.wejuai.core.repository.mysql.AttentionRepository;
import com.wejuai.core.repository.mysql.RewardDemandRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.core.service.dto.HobbyHotType;
import com.wejuai.dto.response.KeyValue;
import com.wejuai.dto.response.UserInfo;
import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mongo.Collection;
import com.wejuai.entity.mongo.Star;
import com.wejuai.entity.mongo.UserPointType;
import com.wejuai.entity.mysql.App;
import com.wejuai.entity.mysql.Article;
import com.wejuai.entity.mysql.GiveType;
import com.wejuai.entity.mysql.Hobby;
import com.wejuai.entity.mysql.RewardDemand;
import com.wejuai.entity.mysql.User;
import com.wejuai.entity.mysql.UserHobby;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wejuai.entity.mongo.AppType.ARTICLE;
import static com.wejuai.entity.mongo.AppType.REWARD_DEMAND;
import static com.wejuai.entity.mysql.GiveType.COLLECT;
import static com.wejuai.entity.mysql.GiveType.DISPLAY;
import static com.wejuai.entity.mysql.GiveType.STAR;

/**
 * @author ZM.Wang
 */
@Service
public class AppBaseService {

    private final UserRepository userRepository;
    private final StarRepository starRepository;
    private final ArticleRepository articleRepository;
    private final AttentionRepository attentionRepository;
    private final CollectionRepository collectionRepository;
    private final RewardDemandRepository rewardDemandRepository;

    private final HobbyService hobbyService;
    private final UserService userService;

    public AppBaseService(UserRepository userRepository, StarRepository starRepository, ArticleRepository articleRepository, AttentionRepository attentionRepository, CollectionRepository collectionRepository, RewardDemandRepository rewardDemandRepository, HobbyService hobbyService, UserService userService) {
        this.userRepository = userRepository;
        this.starRepository = starRepository;
        this.articleRepository = articleRepository;
        this.attentionRepository = attentionRepository;
        this.collectionRepository = collectionRepository;
        this.rewardDemandRepository = rewardDemandRepository;
        this.hobbyService = hobbyService;
        this.userService = userService;
    }

    public UserInfo getOtherUser(String userId, String watchUserId) {
        User otherUser = userService.getUser(userId);
        User watchUser = null;
        if (StringUtils.isNotBlank(watchUserId)) {
            watchUser = userService.getUser(watchUserId);
        }
        boolean myself = otherUser.equals(watchUser);
        boolean follow = watchUser != null && attentionRepository.existsByAttentionAndFollow(watchUser, otherUser);
        return new UserInfo(otherUser, myself).setFollow(follow);
    }

    public Page<KeyValue> getUsersByNickName(String chars, Pageable pageable) {
        if (StringUtils.isBlank(chars)) {
            return userRepository.findAll(pageable).map(KeyValue::new);
        } else {
            return userRepository.findByNickNameLike(chars, pageable).map(KeyValue::new);
        }
    }

    <E extends App<E>> Specification<E> buildSpecification(String titleStr, String userId, String watchUserId, String hobbyId, boolean noHobbyNoData) {
        //观看者是必须要有的，没有在上层就返回空内容了
        User watchUser = userService.getUser(watchUserId);
        UserHobby watchUserHobbies = hobbyService.getHobby(watchUser);

        Set<Hobby> hobbies = new HashSet<>();
        User user = null;
        UserHobby userHobbies = null;
        Hobby hobby = null;

        if (StringUtils.isNotBlank(userId)) {
            user = userService.getUser(userId);
            userHobbies = hobbyService.getHobby(user);
        }

        if (StringUtils.isNotBlank(hobbyId)) {
            hobby = hobbyService.getHobby(hobbyId);
        }

        //用户为空而且爱好为空那就是看他自己，爱好不为空就是在自己个人中心筛选
        if (user == null) {
            if (hobby == null) {
                hobbies.addAll(watchUserHobbies.getHobbies());
            } else {
                if (watchUserHobbies.getHobbies() != null && watchUserHobbies.getHobbies().contains(hobby)) {
                    hobbies.add(hobby);
                }
            }
            //用户不空就是看别人，有爱好就是筛选，没爱好就是看全部
        } else {
            if (hobby == null) {
                if (StringUtils.equals(userId, watchUserId)) {
                    hobbies.addAll(userHobbies.getHobbies());
                } else {
                    hobbies.addAll(userHobbies.getOpenHobbies());
                }
            } else {
                if (userHobbies.getOpenHobbies() != null && userHobbies.getOpenHobbies().contains(hobby)) {
                    hobbies.add(hobby);
                }
            }
        }

        User finalUser = user;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("del"), false));
            if (hobbies.size() == 0 && noHobbyNoData) {
                predicates.add(cb.equal(root.get("id"), "not"));
            } else if (hobbies.size() > 0) {
                CriteriaBuilder.In<Hobby> in = cb.in(root.get("hobby"));
                hobbies.forEach(in::value);
                predicates.add(in);
            }
            if (finalUser != null) {
                predicates.add(cb.equal(root.get("user"), finalUser));
            }
            if (StringUtils.isNotBlank(titleStr)) {
                predicates.add(cb.like(root.get("title"), "%" + titleStr + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 给与参数
     *
     * @param appType  应用类型
     * @param appId    应用id
     * @param userId   用户id
     * @param giveType 给与类型
     */
    @Transactional
    public void giveNum(AppType appType, String appId, String userId, GiveType giveType) {
        App<?> app = getApp(appType, appId);
        if (app != null) {
            if (giveType == STAR) {
                saveApp(app.giveStar());
                giveStar(app, userId);
            } else if (giveType == DISPLAY) {
                saveApp(app.watch());
                giveWatch(app, userId);
            } else if (giveType == COLLECT) {
                saveApp(app.collect());
                giveCollection(app, userId);
            }
        }
    }

    /**
     * 取消给与
     *
     * @param appType  应用类型
     * @param appId    应用id
     * @param userId   用户id
     * @param giveType 给与类型
     */
    @Transactional
    public void reduceNum(AppType appType, String appId, String userId, GiveType giveType) {
        App<?> app = getApp(appType, appId);
        if (app != null) {
            if (giveType == STAR) {
                saveApp(app.unGiveStar());
                reduceStar(app, userId);
            } else if (giveType == COLLECT) {
                saveApp(app.unCollect());
                reduceCollection(app, userId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    <T extends App<T>> App<T> getApp(AppType type, String appId) {
        if (type == ARTICLE) {
            return (T) articleRepository.findById(appId).orElse(null);
        }
        if (type == REWARD_DEMAND) {
            return (T) rewardDemandRepository.findById(appId).orElse(null);
        }
        return null;
    }

    <T extends App<T>> void saveApp(App<T> app) {
        if (app.getAppType() == ARTICLE) {
            articleRepository.save((Article) app);
        }
        if (app.getAppType() == REWARD_DEMAND) {
            assert app instanceof RewardDemand;
            rewardDemandRepository.save((RewardDemand) app);
        }
    }

    private void giveStar(App<?> app, String userId) {
        if (starRepository.existsByUserIdAndAppTypeAndAppId(userId, app.getAppType(), app.getId())) {
            throw new BadRequestException("你已经点赞过了~");
        }
        starRepository.save(new Star(userId, app.getId(), app.getAppType(), app.getUser().getId()));
    }

    private void giveWatch(App<?> app, String userId) {
        new Thread(() -> hobbyService.addHobbyHot(app.getHobby().getId(), HobbyHotType.WATCHED)).start();
        new Thread(() -> userService.addUserPoint(UserPointType.COMMENTED, userId, 1, null)).start();
    }

    private void giveCollection(App<?> app, String userId) {
        if (collectionRepository.existsByUserIdAndAppTypeAndAppId(userId, app.getAppType(), app.getId())) {
            throw new BadRequestException("你已经收藏过了~");
        }
        collectionRepository.save(new Collection(userId, app.getId(), app.getAppType(), app.getUser().getId()));
    }

    private void reduceStar(App<?> app, String userId) {
        Star star = starRepository.findByUserIdAndAppTypeAndAppId(userId, app.getAppType(), app.getId());
        if (star != null) {
            starRepository.delete(star);
        }
    }

    private void reduceCollection(App<?> app, String userId) {
        Collection collection = collectionRepository.findByUserIdAndAppTypeAndAppId(userId, app.getAppType(), app.getId());
        if (collection != null) {
            collectionRepository.delete(collection);
        }
    }

}
