package com.wejuai.core.service;

import com.endofmaster.rest.exception.BadRequestException;
import com.wejuai.core.repository.mongo.CelestialBodyRepository;
import com.wejuai.core.repository.mongo.UserOverflowPointRepository;
import com.wejuai.core.repository.mongo.UserPointRecordRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.core.service.dto.UserPointByDay;
import com.wejuai.entity.mongo.CelestialBody;
import com.wejuai.entity.mongo.UserOverflowPoint;
import com.wejuai.entity.mongo.UserPointRecord;
import com.wejuai.entity.mongo.UserPointType;
import com.wejuai.entity.mysql.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

import static com.wejuai.core.config.Constant.FREE_ITEM_INTEGRAL_UPPER_LIMIT;
import static com.wejuai.core.config.Constant.PAID_ITEM_INTEGRAL_UPPER_LIMIT;
import static com.wejuai.entity.mongo.UserPointType.BE_SELECTED_REWARD_DEMAND;
import static com.wejuai.entity.mongo.UserPointType.BUY_ARTICLE;
import static com.wejuai.entity.mongo.UserPointType.COMMENTED;
import static com.wejuai.entity.mongo.UserPointType.DAILY_LOGIN;
import static com.wejuai.entity.mongo.UserPointType.REWARD_DEMAND_SUCCESSFUL;
import static com.wejuai.entity.mongo.UserPointType.SELL_ARTICLE;
import static com.wejuai.entity.mongo.UserPointType.WATCHED;

/**
 * @author ZM.Wang
 */
@Service
public class UserService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;
    private final CelestialBodyRepository celestialBodyRepository;
    private final UserPointRecordRepository userPointRecordRepository;
    private final UserOverflowPointRepository userOverflowPointRepository;

    private final MongoTemplate mongoTemplate;
    private final CelestialBodyService celestialBodyService;

    public UserService(MongoTemplate mongoTemplate, UserPointRecordRepository userPointRecordRepository, UserOverflowPointRepository userOverflowPointRepository, CelestialBodyRepository celestialBodyRepository, UserRepository userRepository, CelestialBodyService celestialBodyService) {
        this.mongoTemplate = mongoTemplate;
        this.userPointRecordRepository = userPointRecordRepository;
        this.userOverflowPointRepository = userOverflowPointRepository;
        this.celestialBodyRepository = celestialBodyRepository;
        this.userRepository = userRepository;
        this.celestialBodyService = celestialBodyService;
    }

    public void addUserPoint(UserPointType type, String userId, long point, String commentId) {
        if (type == DAILY_LOGIN && !hasUserTodayDailyLogin(userId)) {
            logger.info("用户重复领取每日点数, userId: " + userId);
            throw new BadRequestException("每日登陆点数已领取");
        }
        long userPointByDay = getUserPointByDay(userId, LocalDate.now(), type.getUpperLimitByDay());
        if (userPointByDay > type.getUpperLimitByDay()) {
            //溢出积分记录
            new Thread(() -> userOverflowPointRepository.save(new UserOverflowPoint(userId, type, point))).start();
            throw new RuntimeException("今日领取改类型点数已达上限");
        }
        CelestialBody celestialBody = celestialBodyService.getCelestialBodyByUser(userId);
        celestialBodyRepository.save(celestialBody.addPoint(point));
        UserPointRecord userPointRecord = new UserOverflowPoint(userId, type, point);
        if (type == COMMENTED) {
            userPointRecord.setCommentId(commentId);
        }
        userPointRecordRepository.save(userPointRecord);
    }

    /** 目前只有删除评论 */
    public void subUserPoint(String userId, String commentId) {
        if (userPointRecordRepository.existsByUserAndCommentId(userId, commentId)) {
            userPointRecordRepository.deleteByUserAndCommentId(userId, commentId);
            CelestialBody celestialBody = celestialBodyService.getCelestialBodyByUser(userId);
            celestialBodyRepository.save(celestialBody.cutPoint(1));
        }
    }

    public long getUserPointByDay(String userId, LocalDate date, Long upperLimit) {
        Criteria criteria = new Criteria()
                .and("user").is(userId)
                .and("date").is(date);
        if (upperLimit != null) {
            if (upperLimit == FREE_ITEM_INTEGRAL_UPPER_LIMIT) {
                criteria.and("type").in(WATCHED, COMMENTED);
            }
            if (upperLimit == PAID_ITEM_INTEGRAL_UPPER_LIMIT) {
                criteria.and("type").in(BUY_ARTICLE, SELL_ARTICLE, REWARD_DEMAND_SUCCESSFUL, BE_SELECTED_REWARD_DEMAND);
            }
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("date").sum("point").as("pointByDay")
        );
        AggregationResults<UserPointByDay> aggregate = mongoTemplate.aggregate(aggregation, UserPointRecord.class, UserPointByDay.class);
        UserPointByDay userPointByDay = aggregate.getUniqueMappedResult();
        return userPointByDay == null ? 0 : userPointByDay.getPointByDay();
    }

    public boolean hasUserTodayDailyLogin(String userId) {
        return userPointRecordRepository.existsByUserAndDateAndType(userId, LocalDate.now(), UserPointType.DAILY_LOGIN);
    }

    public User getUser(String userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("没有该用户: " + userId);
            throw new BadRequestException("没有该用户");
        }
        return userOptional.get();
    }

}
