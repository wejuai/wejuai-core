package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.Remind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface RemindRepository extends MongoRepository<Remind, String> {

    Page<Remind> findByRecipient(String recipient, Pageable pageable);

}
