package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.CommentStar;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentStarRepository extends MongoRepository<CommentStar, String> {

    boolean existsByCommentIdAndUserId(String commentId, String userId);
}
