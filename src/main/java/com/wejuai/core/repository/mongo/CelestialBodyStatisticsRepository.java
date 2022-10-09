package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.CelestialBodyStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface CelestialBodyStatisticsRepository extends MongoRepository<CelestialBodyStatistics, String> {
}
