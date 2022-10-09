package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.ArticleDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author ZM.Wang
 */
public interface ArticleDraftRepository extends JpaRepository<ArticleDraft, String>, JpaSpecificationExecutor<ArticleDraft> {
}
