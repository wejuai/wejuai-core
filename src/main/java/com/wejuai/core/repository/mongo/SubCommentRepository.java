package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.SubComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface SubCommentRepository extends MongoRepository<SubComment, String> {

    Page<SubComment> findByCommentId(String commentId, Pageable pageable);

}
