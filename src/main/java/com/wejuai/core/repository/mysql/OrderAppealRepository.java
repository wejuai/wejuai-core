package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.OrderAppeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderAppealRepository extends JpaRepository<OrderAppeal, String>, JpaSpecificationExecutor<OrderAppeal> {

    Page<OrderAppeal> findByUser_Id(String userId, Pageable pageable);
}
