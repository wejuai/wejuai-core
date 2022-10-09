package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.Problem;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProblemRepository extends MongoRepository<Problem, String> {
}
