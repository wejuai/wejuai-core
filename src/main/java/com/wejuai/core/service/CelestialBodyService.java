package com.wejuai.core.service;

import com.endofmaster.commons.aliyun.oss.AliyunOss;
import com.endofmaster.rest.exception.BadRequestException;
import com.wejuai.core.repository.mongo.CelestialBodyRepository;
import com.wejuai.core.repository.mongo.CelestialBodyStatisticsRepository;
import com.wejuai.core.repository.mongo.PointRefreshProblemRepository;
import com.wejuai.core.repository.mongo.UserStarDomainStatisticsRepository;
import com.wejuai.core.repository.mysql.HobbyRepository;
import com.wejuai.core.repository.mysql.UserHobbyRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.dto.response.CelestialBodyImageOssKey;
import com.wejuai.dto.response.CelestialBodyInfo;
import com.wejuai.dto.response.HobbyInfo;
import com.wejuai.dto.response.IdBaseResponse;
import com.wejuai.dto.response.RefreshUserIntegralInfo;
import com.wejuai.entity.mongo.CelestialBody;
import com.wejuai.entity.mongo.CelestialBodyStatistics;
import com.wejuai.entity.mongo.CelestialBodyType;
import com.wejuai.entity.mongo.PointRefreshProblem;
import com.wejuai.entity.mongo.UserPointRecord;
import com.wejuai.entity.mongo.statistics.UserStarDomainStatistics;
import com.wejuai.entity.mysql.Hobby;
import com.wejuai.entity.mysql.User;
import com.wejuai.entity.mysql.UserHobby;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.wejuai.core.config.Constant.CELESTIAL_BODY_STATISTICS_ID;
import static com.wejuai.core.config.Constant.RANDOM;
import static com.wejuai.entity.mongo.CelestialBodyType.HOBBY;
import static com.wejuai.entity.mongo.CelestialBodyType.UNOWNED;
import static com.wejuai.entity.mongo.CelestialBodyType.USER;

/**
 * @author ZM.Wang
 * 星球服务
 */
@Service
public class CelestialBodyService {

    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final CelestialBodyRepository celestialBodyRepository;
    private final PointRefreshProblemRepository pointRefreshProblemRepository;
    private final CelestialBodyStatisticsRepository celestialBodyStatisticsRepository;
    private final UserStarDomainStatisticsRepository userStarDomainStatisticsRepository;

    private final MongoBaseService mongoBaseService;
    private final int coldPlanetCount;
    private final int warmPlanetCount;
    private final int warmTextureCount;
    private final int whiteTextureCount;
    private final int coldTextureCount;

    public CelestialBodyService(UserRepository userRepository, HobbyRepository hobbyRepository, CelestialBodyRepository celestialBodyRepository, PointRefreshProblemRepository pointRefreshProblemRepository, CelestialBodyStatisticsRepository celestialBodyStatisticsRepository, UserHobbyRepository userHobbyRepository, UserStarDomainStatisticsRepository userStarDomainStatisticsRepository, MongoBaseService mongoBaseService, AliyunOss aliyunOss) {
        this.userRepository = userRepository;
        this.hobbyRepository = hobbyRepository;
        this.celestialBodyRepository = celestialBodyRepository;
        this.pointRefreshProblemRepository = pointRefreshProblemRepository;
        this.celestialBodyStatisticsRepository = celestialBodyStatisticsRepository;
        this.userHobbyRepository = userHobbyRepository;
        this.userStarDomainStatisticsRepository = userStarDomainStatisticsRepository;
        this.mongoBaseService = mongoBaseService;
        this.coldPlanetCount = aliyunOss.listObjects("SYS_IMAGE/planet/big-planet-cold-").size();
        this.warmPlanetCount = aliyunOss.listObjects("SYS_IMAGE/planet/big-planet-warm-").size();
        this.warmTextureCount = aliyunOss.listObjects("SYS_IMAGE/planet/big-texture-warm-").size();
        this.whiteTextureCount = aliyunOss.listObjects("SYS_IMAGE/planet/big-texture-white-").size();
        this.coldTextureCount = aliyunOss.listObjects("SYS_IMAGE/planet/big-texture-cold-").size();
    }

    public CelestialBody getCelestialBodyByUser(String userId) {
        userRepository.findById(userId).orElseThrow(() -> new BadRequestException("没有找到该星球对应用户: " + userId));
        CelestialBody celestialBody = celestialBodyRepository.findByUser(userId);
        if (celestialBody == null) {
            celestialBody = saveCelestialBody(USER, userId);
        }
        return celestialBody;
    }

    public CelestialBody getCelestialBodyByHobby(String hobbyId) {
        hobbyRepository.findById(hobbyId).orElseThrow(() -> new BadRequestException("没有找到该星球对应爱好: " + hobbyId));
        CelestialBody celestialBody = celestialBodyRepository.findByHobby(hobbyId);
        if (celestialBody == null) {
            celestialBody = saveCelestialBody(HOBBY, hobbyId);
        }
        return celestialBody;
    }

    public IdBaseResponse createCelestialBody(CelestialBodyType type, String typeId) {
        CelestialBody celestialBody;
        if (type == USER) {
            celestialBody = getCelestialBodyByUser(typeId);
        } else if (type == HOBBY) {
            celestialBody = getCelestialBodyByHobby(typeId);
        } else {
            celestialBody = saveCelestialBody(UNOWNED, null);
        }
        return new IdBaseResponse(celestialBody.getId());
    }

    public List<CelestialBodyInfo> getStarDomain(double minX, double maxX, double minY, double maxY, String watchUserId) {
        List<CelestialBody> celestialBodies = celestialBodyRepository.findByXBetweenAndYBetween(minX, maxX, minY, maxY);
        if (StringUtils.isNotBlank(watchUserId)) {
            new Thread(() -> {
                UserStarDomainStatistics userStarDomainStatistics = userStarDomainStatisticsRepository.findByUserId(watchUserId).orElse(userStarDomainStatisticsRepository.save(new UserStarDomainStatistics(watchUserId)));
                userStarDomainStatisticsRepository.save(userStarDomainStatistics.update(minX, maxX, minY, maxY));
            }).start();
        }
        return celestialBodies.stream().map(celestialBody -> {
            CelestialBodyInfo info = new CelestialBodyInfo(celestialBody);
            if (celestialBody.getType() == USER) {
                User user = userRepository.findById(celestialBody.getUser()).orElseThrow(() -> new RuntimeException("没有找到该星球对应用户: " + celestialBody));
                return info.userInfo(user);
            }
            if (celestialBody.getType() == HOBBY) {
                Hobby hobby = hobbyRepository.findById(celestialBody.getHobby()).orElseThrow(() -> new RuntimeException("没有找到该星球对应爱好: " + celestialBody));
                return info.hobbyInfo(hobby);
            }
            return info;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void updateCelestialBodyPoint(CelestialBodyType type, String id, long point) {
        CelestialBody celestialBody;
        if (type == HOBBY) {
            celestialBody = getCelestialBodyByHobby(id);
        } else if (type == USER) {
            celestialBody = getCelestialBodyByUser(id);
        } else {
            throw new BadRequestException("没有该类型的星球: " + type);
        }
        if (point > 0) {
            celestialBodyRepository.save(celestialBody.addPoint(point));
        } else {
            celestialBodyRepository.save(celestialBody.cutPoint(-point));
        }
    }

    public List<HobbyInfo> getUserCelestialBodyHobbies(String id, String watchUserId) {
        CelestialBody celestialBody = celestialBodyRepository.findById(id).orElseThrow(() -> new BadRequestException("没有该星球"));
        if (celestialBody.getType() != USER) {
            throw new BadRequestException("该星球不是用户星球: " + celestialBody.getType());
        }
        UserHobby userHobby = getUserHobby(celestialBody.getUser());
        if (StringUtils.equals(watchUserId, celestialBody.getUser())) {
            return userHobby.getHobbies().stream().map(HobbyInfo::new).collect(Collectors.toList());
        } else {
            return userHobby.getOpenHobbies().stream().map(HobbyInfo::new).collect(Collectors.toList());
        }
    }

    public CelestialBodyInfo getCelestialBody(String id) {
        CelestialBody celestialBody = celestialBodyRepository.findById(id).orElseThrow(() -> new BadRequestException("没有该星球"));
        CelestialBodyInfo celestialBodyInfo = new CelestialBodyInfo(celestialBody);
        if (celestialBody.getType() == USER) {
            User user = userRepository.findById(celestialBody.getUser()).orElse(null);
            celestialBodyInfo.userInfo(user);
        } else if (celestialBody.getType() == HOBBY) {
            Hobby hobby = hobbyRepository.findById(celestialBody.getHobby()).orElse(null);
            celestialBodyInfo.hobbyInfo(hobby);
        }
        return celestialBodyInfo;
    }

    public CelestialBodyStatistics getCelestialBodyStatistics() {
        Optional<CelestialBodyStatistics> celestialBodyStatistics = celestialBodyStatisticsRepository.findById(CELESTIAL_BODY_STATISTICS_ID);
        return celestialBodyStatistics.orElseGet(() -> celestialBodyStatisticsRepository.save(new CelestialBodyStatistics(CELESTIAL_BODY_STATISTICS_ID)));
    }

    public CelestialBodyImageOssKey getCelestialBodyImageOssKey(String id) {
        CelestialBody celestialBody = celestialBodyRepository.findById(id).orElseThrow(() -> new BadRequestException("没有该星球"));
        return new CelestialBodyImageOssKey(celestialBody);
    }

    public RefreshUserIntegralInfo syncUserPoint(String userId) {
        CelestialBody celestialBody = getCelestialBodyByUser(userId);
        Criteria criteria = new Criteria().and("user").is(userId);
        long actualPoint = mongoBaseService.getMongoSum(criteria, "point", UserPointRecord.class);
        long hasPoint = celestialBody.getPoint();
        if (hasPoint != actualPoint) {
            pointRefreshProblemRepository.save(new PointRefreshProblem(userId, hasPoint, actualPoint));
            celestialBodyRepository.save(celestialBody.setPoint(actualPoint));
            return new RefreshUserIntegralInfo(false, hasPoint, actualPoint);
        }
        return new RefreshUserIntegralInfo(true, hasPoint, actualPoint);
    }

    private CelestialBody saveCelestialBody(CelestialBodyType type, String id) {
        Map<String, Double> coordinate = coordinateCalculation();
        double x = coordinate.get("x");
        double y = coordinate.get("y");
        Map<String, String> appearance = getPlanetAppearance();
        CelestialBody celestialBody = celestialBodyRepository.save(new CelestialBody(type, id, x, y)
                .updateImage(appearance.get("planetKey"), appearance.get("textureKey"), RandomUtils.nextInt(0, 360)));
        CelestialBodyStatistics statistics = getCelestialBodyStatistics();
        celestialBodyStatisticsRepository.save(statistics.addAmount());
        return celestialBody;
    }

    /** 星球坐标生成 */
    private Map<String, Double> coordinateCalculation() {
        //随机坐标轴轴方向
        boolean yDirection = RANDOM.nextBoolean();
        boolean xDirection = RANDOM.nextBoolean();

        CelestialBodyStatistics statistics = getCelestialBodyStatistics();
        long index = statistics.getQuadrantIndex(xDirection, yDirection);

        long width = 4;
        //最大点
        long x = xDirection ? index * width : -(index * width);
        long y = yDirection ? index * width : -(index * width);

        //记录格子
        List<Map<String, Long>> lattices = new ArrayList<>();

        for (int i = 0; i < index; i++) {
            Map<String, Long> map = new HashMap<>(4);
            //纵向格子
            //正的-，负的+，目的是到0是多少格
            long horizontalMinX = xDirection ? x - width : x + width;
            long horizontalMaxY = yDirection ? y - (i * width) : y + (i * width);
            long horizontalMinY = yDirection ? horizontalMaxY - width : horizontalMaxY + width;

            Criteria criteria = new Criteria();
            if (xDirection) {
                criteria.and("x").lte(x).gte(horizontalMinX);
            } else {
                criteria.and("x").lte(horizontalMinX).gte(x);
            }
            if (yDirection) {
                criteria.and("y").lte(horizontalMaxY).gte(horizontalMinY);
            } else {
                criteria.and("y").lte(horizontalMinY).gte(horizontalMaxY);
            }

            long count = mongoBaseService.getMongoPageCount(criteria, CelestialBody.class);
            if (count < 1) {
                map.put("maxX", x);
                map.put("minX", horizontalMinX);
                map.put("maxY", horizontalMaxY);
                map.put("minY", horizontalMinY);
                lattices.add(map);
            }

            //横向格子
            //正的-，负的+，目的是到0是多少格
            if (i != 0) {
                map = new HashMap<>(4);
                long verticalMinY = yDirection ? y - width : y + width;
                long verticalMaxX = xDirection ? x - (i * width) : x + (i * width);
                long verticalMinX = xDirection ? verticalMaxX - width : verticalMaxX + width;

                criteria = new Criteria();
                if (xDirection) {
                    criteria.and("x").lte(verticalMaxX).gte(verticalMinX);
                } else {
                    criteria.and("x").lte(verticalMinX).gte(verticalMaxX);
                }
                if (yDirection) {
                    criteria.and("y").lte(y).gte(verticalMinY);
                } else {
                    criteria.and("y").lte(verticalMinY).gte(y);
                }
                count = mongoBaseService.getMongoPageCount(criteria, CelestialBody.class);
                if (count < 1) {
                    map.put("maxY", y);
                    map.put("minY", verticalMinY);
                    map.put("maxX", verticalMaxX);
                    map.put("minX", verticalMinX);
                    lattices.add(map);
                }
            }
        }

        if (lattices.size() > 0) {
            //随机取一个格子
            int i = RandomUtils.nextInt(0, lattices.size() - 1);
            Map<String, Long> lattice = lattices.get(i);
            //往内缩小取数字范围
            List<Long> xs = Arrays.asList(Math.abs(lattice.get("minX")) + 1, Math.abs(lattice.get("maxX")) - 1);
            Collections.sort(xs);
            double finalX = RandomUtils.nextDouble(xs.get(0), xs.get(1));
            finalX = Double.parseDouble(String.format("%.2f", finalX));
            finalX = xDirection ? finalX : -finalX;
            List<Long> ys = Arrays.asList(Math.abs(lattice.get("minY")) + 1, Math.abs(lattice.get("maxY")) - 1);
            Collections.sort(ys);
            double finalY = RandomUtils.nextDouble(ys.get(0), ys.get(1));
            finalY = Double.parseDouble(String.format("%.2f", finalY));
            finalY = yDirection ? finalY : -finalY;

            Map<String, Double> coordinate = new HashMap<>(2);
            coordinate.put("x", finalX);
            coordinate.put("y", finalY);
            return coordinate;
        } else {
            celestialBodyStatisticsRepository.save(statistics.addQuadrantIndex(xDirection, yDirection));
            return coordinateCalculation();
        }
    }

    /**
     * 星球图片生成<br>
     * <a href="https://gitlab.com/wejuai/wiki/-/wikis/%E6%98%9F%E7%90%83%E5%9B%BE%E7%9B%B8%E5%85%B3%E8%A7%84%E5%88%99">相关文档</a>
     */
    private Map<String, String> getPlanetAppearance() {
        String planetKey = "SYS_IMAGE/planet/big-planet-";
        boolean cold = RandomUtils.nextBoolean();
        if (cold) {
            planetKey = planetKey + "cold-" + RandomUtils.nextInt(1, coldPlanetCount);
        } else {
            planetKey = planetKey + "warm-" + RandomUtils.nextInt(1, warmPlanetCount);
        }
        planetKey += ".png";
        String textureKey = "SYS_IMAGE/planet/big-texture-";
        boolean white = RandomUtils.nextBoolean();
        if (cold) {
            textureKey = textureKey + (white ? "white-" : "cold-") + (white ? RandomUtils.nextInt(1, whiteTextureCount) : RandomUtils.nextInt(1, coldTextureCount));
        } else {
            textureKey = textureKey + (white ? "white-" : "warm-") + (white ? RandomUtils.nextInt(1, whiteTextureCount) : RandomUtils.nextInt(1, warmTextureCount));
        }
        textureKey += ".png";
        Map<String, String> appearance = new HashMap<>(2);
        appearance.put("planetKey", planetKey);
        appearance.put("textureKey", textureKey);
        return appearance;
    }

    private UserHobby getUserHobby(String userId) {
        UserHobby userHobby = userHobbyRepository.findByUser_Id(userId);
        if (userHobby == null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("没有该用户: " + userId));
            userHobby = new UserHobby(user);
        }
        return userHobby;
    }

}
