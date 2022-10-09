package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.RewardDemand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author ZM.Wang
 */
public interface RewardDemandRepository extends JpaRepository<RewardDemand, String>, JpaSpecificationExecutor<RewardDemand> {

    List<RewardDemand> findByDeadlineBefore(LocalDate deadLine);

    boolean existsByIdAndDelFalse(String id);

    Optional<RewardDemand> findByIdAndDelFalse(String id);
}
