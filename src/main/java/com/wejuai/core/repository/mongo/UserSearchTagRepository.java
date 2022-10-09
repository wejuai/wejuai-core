package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.UserSearchTag;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserSearchTagRepository extends MongoRepository<UserSearchTag, String> {
}
