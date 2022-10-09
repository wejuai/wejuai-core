package com.wejuai.core.repository.mongo;

import com.wejuai.entity.mongo.CelestialBody;
import com.wejuai.entity.mongo.CelestialBodyType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CelestialBodyRepository extends MongoRepository<CelestialBody, String> {

    CelestialBody findByUser(String userId);

    CelestialBody findByHobby(String hobbyId);

    List<CelestialBody> findByXBetweenAndYBetween(double minX, double maxX, double minY, double maxY);

    List<CelestialBody> findByType(CelestialBodyType type);
}
