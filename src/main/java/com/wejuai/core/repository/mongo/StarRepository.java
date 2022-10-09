package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mongo.Star;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface StarRepository extends MongoRepository<Star, String> {

    Star findByUserIdAndAppTypeAndAppId(String userId, AppType appType, String appId);

    Page<Star> findByUserIdAndAppType(String userId, AppType appType, Pageable pageable);

    boolean existsByUserIdAndAppTypeAndAppId(String userId, AppType appType, String appId);

    void deleteAllByAppTypeAndAppId(AppType appType, String appId);
}
