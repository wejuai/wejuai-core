package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.trade.Charge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChargeRepository extends MongoRepository<Charge, String> {

    List<Charge> findByUserId(String userId);
}
