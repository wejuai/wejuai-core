package com.wejuai.core.service;

import com.wejuai.core.repository.mongo.OrdersStatisticsRepository;
import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mongo.statistics.OrdersStatistics;
import org.springframework.stereotype.Service;

import static com.wejuai.core.config.Constant.ORDERS_STATISTICS_ID;
import static com.wejuai.entity.mongo.AppType.ARTICLE;
import static com.wejuai.entity.mongo.AppType.REWARD_DEMAND;

/**
 * @author ZM.Wang
 */
@Service
public class StatisticsService {

    private final OrdersStatisticsRepository ordersStatisticsRepository;

    public StatisticsService(OrdersStatisticsRepository ordersStatisticsRepository) {
        this.ordersStatisticsRepository = ordersStatisticsRepository;
    }

    public void addAppOrders(AppType type, long amount) {
        if (type == ARTICLE) {
            ordersStatisticsRepository
                    .save(getOrders().addArticleCount().addArticleAmount(amount));
        }
        if (type == REWARD_DEMAND) {
            ordersStatisticsRepository
                    .save(getOrders().addRewardDemandCount().addRewardDemandAmount(amount));
        }
    }

    public void addTransferOrders(boolean income, long amount) {
        if (income) {
            ordersStatisticsRepository
                    .save(getOrders().addTransferAddCount().addTransferAddAmount(amount));
        } else {
            ordersStatisticsRepository
                    .save(getOrders().addTransferSubCount().addTransferSubAmount(amount));
        }
    }

    private OrdersStatistics getOrders() {
        return ordersStatisticsRepository
                .findById(ORDERS_STATISTICS_ID)
                .orElse(ordersStatisticsRepository.save(new OrdersStatistics(ORDERS_STATISTICS_ID)));
    }

}
