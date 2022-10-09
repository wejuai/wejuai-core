package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, String>, JpaSpecificationExecutor<Withdrawal> {

    @Query(nativeQuery = true,value = "SELECT IFNULL(SUM(integral),0) FROM withdrawal WHERE `status`='UNTREATED' and user_id=?1")
    long sumUserProcessIntegral(String userId);
}
