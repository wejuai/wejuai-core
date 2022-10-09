package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.Region;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface RegionRepository extends MongoRepository<Region, String> {
}
