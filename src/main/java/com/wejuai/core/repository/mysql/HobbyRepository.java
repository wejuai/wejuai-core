package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.Hobby;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author ZM.Wang
 */
public interface HobbyRepository extends JpaRepository<Hobby, String> {

    Page<Hobby> findByIdNotIn(List<String> ids, Pageable pageable);
}
