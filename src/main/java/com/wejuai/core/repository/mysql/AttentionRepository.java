package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.Attention;
import com.wejuai.entity.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author ZM.Wang
 */
public interface AttentionRepository extends JpaRepository<Attention, String> {

    boolean existsByAttentionAndFollow(User attention, User follow);
}
