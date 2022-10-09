package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.statistics.HobbyHot;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HobbyHotRepository extends MongoRepository<HobbyHot, String> {

    HobbyHot findByHobbyId(String hobbyId);
}
