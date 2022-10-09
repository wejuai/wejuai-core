package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.ApplyCancelRewardRemand;
import com.wejuai.entity.mysql.ApplyStatus;
import com.wejuai.entity.mysql.RewardDemand;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author ZM.Wang
 */
public interface ApplyCancelRewardRemandRepository extends JpaRepository<ApplyCancelRewardRemand, String> {

    boolean existsByRewardDemandAndStatus(RewardDemand rewardDemand, ApplyStatus status);
}
