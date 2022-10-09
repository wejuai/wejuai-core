package com.wejuai.core.service;

import com.wejuai.core.repository.mongo.CollectionRepository;
import com.wejuai.core.repository.mongo.StarRepository;
import com.wejuai.core.service.dto.MongoCount;
import com.wejuai.entity.mysql.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ZM.Wang
 */
@Service
public class MongoBaseService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MongoTemplate mongoTemplate;
    private final StarRepository starRepository;
    private final CollectionRepository collectionRepository;

    public MongoBaseService(MongoTemplate mongoTemplate, StarRepository starRepository, CollectionRepository collectionRepository) {
        this.mongoTemplate = mongoTemplate;
        this.starRepository = starRepository;
        this.collectionRepository = collectionRepository;
    }

    long getMongoPageCount(Criteria criteria, Class<?> clazz) {
        Aggregation countAggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().count().as("count")
        );
        AggregationResults<MongoCount> countAggregate = mongoTemplate.aggregate(countAggregation, clazz, MongoCount.class);
        MongoCount mongoCount = countAggregate.getUniqueMappedResult();
        return mongoCount == null ? 0 : mongoCount.getCount();
    }

    @SuppressWarnings("SameParameterValue")
    long getMongoSum(Criteria criteria, String sumField, Class<?> clazz) {
        Aggregation countAggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum(sumField).as("count")
        );
        AggregationResults<MongoCount> countAggregate = mongoTemplate.aggregate(countAggregation, clazz, MongoCount.class);
        MongoCount mongoCount = countAggregate.getUniqueMappedResult();
        return mongoCount == null ? 0 : mongoCount.getCount();
    }

    void delAllStarAndCollection(App<?> app, int count) {
        try {
            starRepository.deleteAllByAppTypeAndAppId(app.getAppType(), app.getId());
            collectionRepository.deleteAllByAppTypeAndAppId(app.getAppType(), app.getId());
        } catch (Exception e) {
            if (count > 4) {
                logger.error("多次删除应用的所有关注和收藏错误", e);
                return;
            }
            delAllStarAndCollection(app, ++count);
        }
    }

    @SuppressWarnings("SameParameterValue")
    <T> List<T> getList(Criteria criteria, long page, long size, Class<T> clazz, Sort.Direction sort, String... sortParam) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sort(sort, sortParam),
                Aggregation.skip(page * size),
                Aggregation.limit(size)
        );
        AggregationResults<T> aggregate = mongoTemplate.aggregate(aggregation, clazz, clazz);
        return aggregate.getMappedResults();
    }
}
