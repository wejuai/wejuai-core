package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.Province;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface ProvinceRepository extends MongoRepository<Province, String> {
}
