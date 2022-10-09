package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.SystemMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface SystemMessageRepository extends MongoRepository<SystemMessage, String> {

    Page<SystemMessage> findByUserId(String userId, Pageable pageable);
}
