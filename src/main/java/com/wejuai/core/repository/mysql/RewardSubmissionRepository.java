package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.RewardDemand;
import com.wejuai.entity.mysql.RewardSubmission;
import com.wejuai.entity.mysql.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * @author ZM.Wang
 */
public interface RewardSubmissionRepository extends JpaRepository<RewardSubmission, String>, JpaSpecificationExecutor<RewardSubmission> {

    List<RewardSubmission> findByRewardDemand(RewardDemand rewardDemand);

    List<RewardSubmission> findByRewardDemandAndUser(RewardDemand rewardDemand, User user);

    RewardSubmission findByRewardDemandAndSelectedTrue(RewardDemand rewardDemand);

    long countByRewardDemand(RewardDemand rewardDemand);

    Page<RewardSubmission> findByRewardDemand_Id(String rewardDemandId, Pageable pageable);

    boolean existsByRewardDemandAndUser_Id(RewardDemand rewardDemand, String userId);
}
