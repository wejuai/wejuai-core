package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mysql.Evaluate;
import com.wejuai.entity.mysql.Orders;
import com.wejuai.entity.mysql.RewardSubmission;
import com.wejuai.entity.mysql.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author ZM.Wang
 */
public interface EvaluateRepository extends JpaRepository<Evaluate, String> {

    long countByOrdersAndEvaluator(Orders orders, User evaluator);

    long countByRewardSubmissionAndEvaluator(RewardSubmission rewardSubmission, User evaluator);

    Evaluate findByAppTypeAndRewardSubmission_Id(AppType type, String submissionId);

    Page<Evaluate> findByAppTypeAndOrders_AppId(AppType appType, String appId, Pageable pageable);
}
