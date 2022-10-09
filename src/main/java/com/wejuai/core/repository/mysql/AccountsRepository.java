package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.Accounts;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by ZM.Wang
 */
public interface AccountsRepository extends JpaRepository<Accounts, String> {
}
