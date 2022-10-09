package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mysql.Orders;
import com.wejuai.entity.mysql.OrdersType;
import com.wejuai.entity.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

/**
 * @author ZM.Wang
 */
public interface OrdersRepository extends JpaRepository<Orders, String>, JpaSpecificationExecutor<Orders> {

    long countByUser_IdAndAppIdAndAppTypeAndNullifyFalse(String userId, String appId, AppType type);

    Orders findByUserAndAppTypeAndAppIdAndNullifyFalse(User user, AppType appType, String appId);

    List<Orders> findByAppTypeAndAppIdAndIncomeFalseAndIntegralAfterAndNullifyFalse(AppType appType, String appId, long integral);

    @Query(nativeQuery = true, value = "select IFNULL(sum(integral),0) from orders where app_type=?1 and app_id=?2 and integral>0")
    long sumAppOrdersIntegral(AppType appType, String appId);

    /** 范围内部分订单类型积分统计 */
    @Query(nativeQuery = true, value = "SELECT IFNULL(sum(integral),0) integral from orders where nullify=false and income=true and integral>0 and user_id=?1 and created_at BETWEEN ?2 and ?3 and type in ?4")
    long sumUserAddIntegral(String userId, Date start, Date end, List<OrdersType> types);

    /** 用户总收入或者总支出 */
    @Query(nativeQuery = true, value = "SELECT IFNULL(sum(integral),0) integral from orders where income=?1 AND integral>0 and user_id=?2 and nullify=false")
    long sumUserIntegral(boolean income, String userId);



}