package com.wejuai.core.config;

import com.endofmaster.commons.aliyun.oss.AliyunOss;
import com.endofmaster.commons.id.IdGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.wejuai.core.repository.mongo.CelestialBodyStatisticsRepository;
import com.wejuai.core.repository.mongo.CityRepository;
import com.wejuai.core.repository.mongo.HobbyTabRepository;
import com.wejuai.core.repository.mongo.ProvinceRepository;
import com.wejuai.core.repository.mongo.RegionRepository;
import com.wejuai.core.repository.mysql.AccountsRepository;
import com.wejuai.core.repository.mysql.HobbyRepository;
import com.wejuai.core.repository.mysql.ImageRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.entity.mongo.CelestialBodyStatistics;
import com.wejuai.entity.mongo.City;
import com.wejuai.entity.mongo.HobbyTab;
import com.wejuai.entity.mongo.Province;
import com.wejuai.entity.mongo.Region;
import com.wejuai.entity.mysql.Accounts;
import com.wejuai.entity.mysql.Hobby;
import com.wejuai.entity.mysql.Image;
import com.wejuai.entity.mysql.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.wejuai.core.config.Constant.CELESTIAL_BODY_STATISTICS_ID;
import static com.wejuai.core.config.Constant.MAPPER;
import static com.wejuai.entity.mysql.ImageUploadType.SYS_IMAGE;

/**
 * @author ZM.Wang
 */
@Order(200)
@Configuration
public class DataInitConfig {

    private final static String SYSTEM_USER_ID = "system";

    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final HobbyRepository hobbyRepository;
    private final RegionRepository regionRepository;
    private final AccountsRepository accountsRepository;
    private final ProvinceRepository provinceRepository;
    private final HobbyTabRepository hobbyTabRepository;
    private final CelestialBodyStatisticsRepository celestialBodyStatisticsRepository;

    private final AliyunOss aliyunOss;

    public DataInitConfig(UserRepository userRepository, ImageRepository imageRepository, HobbyRepository hobbyRepository, ProvinceRepository provinceRepository, CityRepository cityRepository, RegionRepository regionRepository, AccountsRepository accountsRepository, HobbyTabRepository hobbyTabRepository, CelestialBodyStatisticsRepository celestialBodyStatisticsRepository, AliyunOss aliyunOss) {
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
        this.hobbyRepository = hobbyRepository;
        this.provinceRepository = provinceRepository;
        this.cityRepository = cityRepository;
        this.regionRepository = regionRepository;
        this.accountsRepository = accountsRepository;
        this.hobbyTabRepository = hobbyTabRepository;
        this.celestialBodyStatisticsRepository = celestialBodyStatisticsRepository;
        this.aliyunOss = aliyunOss;
    }

    @PostConstruct
    void initCelestialBodyImage() {
        aliyunOss.listObjects("SYS_IMAGE/celestialBody/big-").forEach(imageObject -> {
            String ossKey = imageObject.getKey();
            if (!imageRepository.existsByTypeAndOssKey(SYS_IMAGE, ossKey)) {
                imageRepository.save(new Image(ossKey, SYS_IMAGE));
            }
            ossKey = ossKey.replaceAll("big", "small");
            if (!imageRepository.existsByTypeAndOssKey(SYS_IMAGE, ossKey)) {
                imageRepository.save(new Image(ossKey, SYS_IMAGE));
            }
        });
    }

    @PostConstruct
    void initCelestialBodyStatistics() {
        Optional<CelestialBodyStatistics> celestialBodyStatistics = celestialBodyStatisticsRepository.findById(CELESTIAL_BODY_STATISTICS_ID);
        if (celestialBodyStatistics.isEmpty()) {
            celestialBodyStatisticsRepository.save(new CelestialBodyStatistics(CELESTIAL_BODY_STATISTICS_ID));
        }
    }

    @PostConstruct
    void initHobby() {
        imageRepository.save(new Image("defHeadImage", "SYS_IMAGE/defHeadImage.jpg", SYS_IMAGE));
        imageRepository.save(new Image("logo", "SYS_IMAGE/logo.png", SYS_IMAGE));
        imageRepository.save(new Image("indexBackground", "SYS_IMAGE/indexBackground.png", SYS_IMAGE));
        Image nba = imageRepository.save(new Image("nba", "SYS_IMAGE/nba.png", SYS_IMAGE));
        Image it = imageRepository.save(new Image("it", "SYS_IMAGE/it.png", SYS_IMAGE));
        Image curio = imageRepository.save(new Image("curio", "SYS_IMAGE/curio.png", SYS_IMAGE));
        hobbyRepository.save(new Hobby("it", "互联网", "it.wejuai.com").setAvatar(it));
        hobbyRepository.save(new Hobby("nba", "NBA", "nba.wejuai.com").setAvatar(nba));
        hobbyRepository.save(new Hobby("curio", "古玩", "curio.wejuai.com").setAvatar(curio));
    }

    @PostConstruct
    void initHobbyTab() {
        if (hobbyTabRepository.count() < 37) {
            hobbyTabRepository.deleteAll();
            List<HobbyTab> hobbyTabs = new ArrayList<>();
            hobbyTabs.add(new HobbyTab(IdGenerator.objectId(), "IT", "it"));
            hobbyTabs.add(new HobbyTab(IdGenerator.objectId(), "码农", "it"));
            hobbyTabs.add(new HobbyTab(IdGenerator.objectId(), "程序员", "it"));
            hobbyTabs.add(new HobbyTab(IdGenerator.objectId(), "CURIO", "curio"));
            hobbyTabs.add(new HobbyTab(IdGenerator.objectId(), "古玩", "curio"));
            hobbyTabs.add(new HobbyTab(IdGenerator.objectId(), "篮球", "nba"));
            hobbyTabs.add(new HobbyTab(IdGenerator.objectId(), "NBA", "nba"));
            hobbyTabRepository.saveAll(hobbyTabs);
        }
    }

    @PostConstruct
    void initProvince() throws IOException {
        if (provinceRepository.count() < 1) {
            InputStream file = this.getClass().getResourceAsStream("/province.json");
            List<Province> provinces = MAPPER.readValue(file, new TypeReference<>() {
            });
            provinceRepository.saveAll(provinces);
        }
    }

    @PostConstruct
    void initCity() throws IOException {
        if (cityRepository.count() < 1) {
            InputStream file = this.getClass().getResourceAsStream("/city.json");
            List<City> cities = MAPPER.readValue(file, new TypeReference<>() {
            });
            cityRepository.saveAll(cities);
        }
    }

    @PostConstruct
    void initRegion() throws IOException {
        if (regionRepository.count() < 1) {
            InputStream file = this.getClass().getResourceAsStream("/city.json");
            List<Region> regions = MAPPER.readValue(file, new TypeReference<>() {
            });
            regionRepository.saveAll(regions);
        }
    }

    @PostConstruct
    @Transactional
    void initSystemUser() {
        Accounts accounts;
        if (accountsRepository.existsById(SYSTEM_USER_ID)) {
            accounts = accountsRepository.findById(SYSTEM_USER_ID).orElseThrow();
        } else {
            accounts = accountsRepository.save(new Accounts(SYSTEM_USER_ID, "帐号", "密码"));
        }
        if (!userRepository.existsById(SYSTEM_USER_ID)) {
            userRepository.save(new User(SYSTEM_USER_ID, accounts));
        }
    }
}
