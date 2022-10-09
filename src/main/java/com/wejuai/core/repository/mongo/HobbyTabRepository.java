package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.HobbyTab;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HobbyTabRepository extends MongoRepository<HobbyTab, String> {

    List<HobbyTab> findByTab(String tab);

    List<HobbyTab> findByTabLike(String tab);
}
