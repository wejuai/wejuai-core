package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


/**
 * @author ZM.Wang
 */
public interface UserRepository extends JpaRepository<User, String> {

    Page<User> findByNickNameLike(String chars, Pageable pageable);

    long countById(String id);
}
