package com.wejuai.core.service;

import com.endofmaster.rest.exception.BadRequestException;
import com.endofmaster.txAi.TxAiClient;
import com.endofmaster.txAi.nlp.TbpTextProcessRequest;
import com.endofmaster.txAi.nlp.TbpTextProcessResponse;
import com.wejuai.core.repository.mongo.HobbyHotByDayRepository;
import com.wejuai.core.repository.mongo.HobbyHotRepository;
import com.wejuai.core.repository.mongo.HobbyTabRepository;
import com.wejuai.core.repository.mongo.HobbyTotalHotByDayRepository;
import com.wejuai.core.repository.mongo.HobbyTotalHotRepository;
import com.wejuai.core.repository.mongo.UserSearchTagRepository;
import com.wejuai.core.repository.mysql.HobbyRepository;
import com.wejuai.core.repository.mysql.UserHobbyRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.core.service.dto.HobbyHotType;
import com.wejuai.dto.response.GetHobbyDomain;
import com.wejuai.entity.mongo.CelestialBodyType;
import com.wejuai.entity.mongo.HobbyTab;
import com.wejuai.entity.mongo.UserSearchTag;
import com.wejuai.entity.mongo.statistics.HobbyHot;
import com.wejuai.entity.mongo.statistics.HobbyHotByDay;
import com.wejuai.entity.mongo.statistics.HobbyTotalHot;
import com.wejuai.entity.mongo.statistics.HobbyTotalHotByDay;
import com.wejuai.entity.mysql.Hobby;
import com.wejuai.entity.mysql.User;
import com.wejuai.entity.mysql.UserHobby;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * @author ZM.Wang
 */
@Service
public class HobbyService {

    private final static String HOBBY_TOTAL_HOT_ID = "hobbyTotalHot";

    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final HobbyTabRepository hobbyTabRepository;
    private final HobbyHotRepository hobbyHotRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final HobbyTotalHotRepository hobbyTotalHotRepository;
    private final HobbyHotByDayRepository hobbyHotByDayRepository;
    private final UserSearchTagRepository userSearchTagRepository;
    private final HobbyTotalHotByDayRepository hobbyTotalHotByDayRepository;

    private final TxAiClient txAiClient;
    private final UserService userService;
    private final CelestialBodyService celestialBodyService;

    public HobbyService(HobbyRepository hobbyRepository, HobbyTabRepository hobbyTabRepository, UserHobbyRepository userHobbyRepository, UserRepository userRepository, HobbyHotRepository hobbyHotRepository, HobbyTotalHotRepository hobbyTotalHotRepository, HobbyHotByDayRepository hobbyHotByDayRepository, UserSearchTagRepository userSearchTagRepository, HobbyTotalHotByDayRepository hobbyTotalHotByDayRepository, CelestialBodyService celestialBodyService, UserService userService, TxAiClient txAiClient) {
        this.hobbyRepository = hobbyRepository;
        this.hobbyTabRepository = hobbyTabRepository;
        this.userHobbyRepository = userHobbyRepository;
        this.userRepository = userRepository;
        this.hobbyHotRepository = hobbyHotRepository;
        this.hobbyTotalHotRepository = hobbyTotalHotRepository;
        this.hobbyHotByDayRepository = hobbyHotByDayRepository;
        this.userSearchTagRepository = userSearchTagRepository;
        this.hobbyTotalHotByDayRepository = hobbyTotalHotByDayRepository;
        this.celestialBodyService = celestialBodyService;
        this.userService = userService;
        this.txAiClient = txAiClient;
    }

    public Hobby getHobby(String id) {
        return hobbyRepository.findById(id).orElseThrow(() -> new BadRequestException("没有该爱好: " + id));
    }

    @Transactional
    public void followHobby(String userId, String id) {
        User user = userService.getUser(userId);
        Hobby hobby = getHobby(id);
        UserHobby userHobby = getUserHobby(userId);
        userHobbyRepository.save(userHobby.addHobby(hobby));
        userRepository.save(user.addHobby());

        new Thread(() -> addHobbyHot(id, HobbyHotType.FOLLOWED)).start();
    }

    @Transactional
    public void unfollowHobby(String userId, String id) {
        User user = userService.getUser(userId);
        Hobby hobby = getHobby(id);
        UserHobby userHobby = getUserHobby(userId);
        userHobbyRepository.save(userHobby.reduceHobby(hobby));
        userRepository.save(user.reduceHobby());

        new Thread(() -> subHobbyHot(id, HobbyHotType.FOLLOWED)).start();
    }

    /**
     * 获取用户关注爱好
     *
     * @param userId      获取该用户的列表
     * @param watchUserId 调用接口用户
     */
    public Set<Hobby> getHobbies(String userId, String watchUserId) {
        UserHobby userHobby = getUserHobby(userId);
        if (StringUtils.equals(userId, watchUserId)) {
            return userHobby.getHobbies();
        } else {
            return userHobby.getOpenHobbies();
        }
    }

    public UserHobby getHobby(User user) {
        UserHobby userHobby = userHobbyRepository.findByUser(user);
        if (userHobby == null) {
            userHobby = new UserHobby(user);
        }
        return userHobby;
    }

    public void addHobbyHot(String hobbyId, HobbyHotType type) {
        getHobby(hobbyId);
        HobbyHot hobbyHot = getHobbyHot(hobbyId);
        HobbyHotByDay hobbyHotByDay = getHobbyHotByDay(hobbyId, LocalDate.now());
        HobbyTotalHotByDay hobbyTotalHotByDay = getHobbyTotalHotByDay(LocalDate.now());
        HobbyTotalHot hobbyTotalHot = hobbyTotalHotRepository.findById(HOBBY_TOTAL_HOT_ID).orElse(new HobbyTotalHot(HOBBY_TOTAL_HOT_ID));
        switch (type) {
            case WATCHED:
                hobbyHotRepository.save(hobbyHot.addWatched());
                hobbyHotByDayRepository.save(hobbyHotByDay.addWatched());
                hobbyTotalHotRepository.save(hobbyTotalHot.addWatched(1));
                hobbyTotalHotByDayRepository.save(hobbyTotalHotByDay.addWatched());
                break;
            case COMMENTED:
                hobbyHotRepository.save(hobbyHot.addCommented());
                hobbyHotByDayRepository.save(hobbyHotByDay.addCommented());
                hobbyTotalHotRepository.save(hobbyTotalHot.addCommented(1));
                hobbyTotalHotByDayRepository.save(hobbyTotalHotByDay.addCommented());
                break;
            case CREATED:
                hobbyHotRepository.save(hobbyHot.addCreated());
                hobbyHotByDayRepository.save(hobbyHotByDay.addCreated());
                hobbyTotalHotRepository.save(hobbyTotalHot.addCreated(1));
                hobbyTotalHotByDayRepository.save(hobbyTotalHotByDay.addCreated());
                break;
            case FOLLOWED:
                hobbyHotRepository.save(hobbyHot.addFollowed());
                hobbyHotByDayRepository.save(hobbyHotByDay.addFollowed());
                hobbyTotalHotRepository.save(hobbyTotalHot.addFollowed(1));
                hobbyTotalHotByDayRepository.save(hobbyTotalHotByDay.addFollowed());
                break;
            default:
                throw new RuntimeException("没有该热度类型: " + type);
        }
        celestialBodyService.updateCelestialBodyPoint(CelestialBodyType.HOBBY, hobbyId, type.getPoint());
    }

    public void subHobbyHot(String hobbyId, HobbyHotType type) {
        getHobby(hobbyId);
        HobbyHot hobbyHot = getHobbyHot(hobbyId);
        HobbyHotByDay hobbyHotByDay = getHobbyHotByDay(hobbyId, LocalDate.now());
        HobbyTotalHotByDay hobbyTotalHotByDay = getHobbyTotalHotByDay(LocalDate.now());
        HobbyTotalHot hobbyTotalHot = hobbyTotalHotRepository.findById(HOBBY_TOTAL_HOT_ID).orElse(new HobbyTotalHot(HOBBY_TOTAL_HOT_ID));
        switch (type) {
            case COMMENTED:
                hobbyHotRepository.save(hobbyHot.subCommented());
                hobbyHotByDayRepository.save(hobbyHotByDay.subCommented());
                hobbyTotalHotRepository.save(hobbyTotalHot.addCommented(-1));
                hobbyTotalHotByDayRepository.save(hobbyTotalHotByDay.subCommented());
                break;
            case CREATED:
                hobbyHotRepository.save(hobbyHot.subCreated());
                hobbyHotByDayRepository.save(hobbyHotByDay.subCreated());
                hobbyTotalHotRepository.save(hobbyTotalHot.addCreated(-1));
                hobbyTotalHotByDayRepository.save(hobbyTotalHotByDay.subCreated());
                break;
            case FOLLOWED:
                hobbyHotRepository.save(hobbyHot.addUnfollowed());
                hobbyHotByDayRepository.save(hobbyHotByDay.addUnfollowed());
                hobbyTotalHotRepository.save(hobbyTotalHot.addFollowed(-1));
                hobbyTotalHotByDayRepository.save(hobbyTotalHotByDay.addUnfollowed());
                break;
            default:
                throw new RuntimeException("该热度类型无法减少: " + type);
        }
        celestialBodyService.updateCelestialBodyPoint(CelestialBodyType.HOBBY, hobbyId, -type.getPoint());
    }

    /** 搜索爱好 */
    public GetHobbyDomain getHobbyDomainByTab(String userId, String tab) {
        if (StringUtils.isBlank(tab)) {
            throw new BadRequestException("没有输入有效内容~");
        }
        tab = tab.toUpperCase();
        List<HobbyTab> hobbyTabs = hobbyTabRepository.findByTab(tab);
        if (hobbyTabs.size() == 1) {
            return new GetHobbyDomain(getHobby(hobbyTabs.get(0).getHobbyId()));
        } else if (hobbyTabs.size() > 1) {
            throw new BadRequestException("请缩小范围哦~~");
        } else {
            hobbyTabs = hobbyTabRepository.findByTabLike(tab);
            if (hobbyTabs.size() == 1) {
                return new GetHobbyDomain(getHobby(hobbyTabs.get(0).getHobbyId()));
            } else if (hobbyTabs.size() > 1) {
                throw new BadRequestException("请缩小范围哦~~");
            } else {
                userSearchTagRepository.save(new UserSearchTag(tab));
                return new GetHobbyDomain(getAnswer(userId, tab));
            }
        }
    }

    private UserHobby getUserHobby(String userId) {
        UserHobby userHobby = userHobbyRepository.findByUser_Id(userId);
        if (userHobby == null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("没有该用户: " + userId));
            userHobby = new UserHobby(user);
        }
        return userHobby;
    }

    private HobbyHot getHobbyHot(String hobbyId) {
        HobbyHot hobbyHot = hobbyHotRepository.findByHobbyId(hobbyId);
        if (hobbyHot == null) {
            hobbyHot = hobbyHotRepository.save(new HobbyHot(hobbyId));
        }
        return hobbyHot;
    }

    private HobbyHotByDay getHobbyHotByDay(String hobbyId, LocalDate localDate) {
        HobbyHotByDay hobbyHotByDay = hobbyHotByDayRepository.findByHobbyIdAndDate(hobbyId, localDate);
        if (hobbyHotByDay == null) {
            hobbyHotByDay = hobbyHotByDayRepository.save(new HobbyHotByDay(hobbyId, localDate));
        }
        return hobbyHotByDay;
    }

    private HobbyTotalHotByDay getHobbyTotalHotByDay(LocalDate localDate) {
        HobbyTotalHotByDay hobbyTotalHotByDay = hobbyTotalHotByDayRepository.findByDate(localDate);
        if (hobbyTotalHotByDay == null) {
            hobbyTotalHotByDay = hobbyTotalHotByDayRepository.save(new HobbyTotalHotByDay(localDate));
        }
        return hobbyTotalHotByDay;
    }

    private String getAnswer(String id, String text) {
        TbpTextProcessRequest request = new TbpTextProcessRequest("c01e5a72-312a-4ef1-b037-c2ace3c2949c", id, text);
        TbpTextProcessResponse response = txAiClient.execute(request);
        if (response.successful()) {
            return response.getAnswer();
        } else {
            throw new BadRequestException("没有结果哦~");
        }
    }

}
