package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mongo.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface CommentRepository extends MongoRepository<Comment, String> {

    Page<Comment> findByAppTypeAndAppId(AppType appType, String appId, Pageable pageable);

    Page<Comment> findByAppCreator(String appCreator, Pageable pageable);
}
