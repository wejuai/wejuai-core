package com.wejuai.core.service;

import com.wejuai.core.repository.mongo.CelestialBodyRepository;
import com.wejuai.entity.mongo.CelestialBody;
import com.wejuai.entity.mongo.CelestialBodyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest(properties = "spring.profiles.active=dev")
class CelestialBodyServiceTest {

    @Autowired
    private CelestialBodyRepository celestialBodyRepository;

    @Test
    public void updatePoint() {
        List<CelestialBody> byType = celestialBodyRepository.findByType(CelestialBodyType.UNOWNED);
        byType = byType.stream().map(celestialBody -> celestialBody.setPoint(0)).collect(Collectors.toList());
        celestialBodyRepository.saveAll(byType);

    }

}