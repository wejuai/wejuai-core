package com.wejuai.core.repository.mysql;

import com.wejuai.entity.mysql.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Created by ZM.Wang
 */
public interface ArticleRepository extends JpaRepository<Article, String>, JpaSpecificationExecutor<Article> {

}
