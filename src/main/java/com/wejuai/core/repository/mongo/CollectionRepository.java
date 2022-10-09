package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mongo.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author ZM.Wang
 */
public interface CollectionRepository extends MongoRepository<Collection, String> {

    Collection findByUserIdAndAppTypeAndAppId(String userId, AppType appType, String appId);

    Page<Collection> findByUserIdAndAppType(String userId, AppType appType, Pageable pageable);

    boolean existsByUserIdAndAppTypeAndAppId(String userId, AppType appType, String appId);

    void deleteAllByAppTypeAndAppId(AppType appType, String appId);
}
