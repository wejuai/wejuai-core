package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.statistics.UserStarDomainStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserStarDomainStatisticsRepository extends MongoRepository<UserStarDomainStatistics, String> {

    Optional<UserStarDomainStatistics> findByUserId(String userId);
}
