package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.statistics.HobbyHotByDay;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;

/**
 * @author ZM.Wang
 */
public interface HobbyHotByDayRepository extends MongoRepository<HobbyHotByDay, String> {

    HobbyHotByDay findByHobbyIdAndDate(String hobbyId, LocalDate localDate);
}
