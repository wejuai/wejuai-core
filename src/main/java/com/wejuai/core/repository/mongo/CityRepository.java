package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.City;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface CityRepository extends MongoRepository<City, String> {
}
