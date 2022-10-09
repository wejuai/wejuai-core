package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.statistics.OrdersStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrdersStatisticsRepository extends MongoRepository<OrdersStatistics, String> {
}
