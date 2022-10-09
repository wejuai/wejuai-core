package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.UserPointRecord;
import com.wejuai.entity.mongo.UserPointType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;

public interface UserPointRecordRepository extends MongoRepository<UserPointRecord, String> {

    boolean existsByUserAndDateAndType(String userId, LocalDate date, UserPointType type);

    boolean existsByUserAndCommentId(String userId, String commentId);

    void deleteByUserAndCommentId(String userId, String commentId);

}
