package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.UserOverflowPoint;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserOverflowPointRepository extends MongoRepository<UserOverflowPoint, String> {
}
