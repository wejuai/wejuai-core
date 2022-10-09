package com.wejuai.core.web;

import com.wejuai.core.service.CelestialBodyService;
import com.wejuai.core.service.HobbyService;
import com.wejuai.core.service.UserService;
import com.wejuai.dto.request.SaveCelestialBodyRequest;
import com.wejuai.dto.response.CelestialBodyImageOssKey;
import com.wejuai.dto.response.CelestialBodyInfo;
import com.wejuai.dto.response.HobbyInfo;
import com.wejuai.dto.response.IdBaseResponse;
import com.wejuai.dto.response.RefreshUserIntegralInfo;
import com.wejuai.entity.mongo.CelestialBody;
import com.wejuai.entity.mysql.Hobby;
import com.wejuai.entity.mysql.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ZM.Wang
 */
@Api(tags = "星球相关")
@RestController
@RequestMapping("/celestialBody")
public class CelestialBodyController {

    private final CelestialBodyService celestialBodyService;
    private final HobbyService hobbyService;
    private final UserService userService;

    public CelestialBodyController(CelestialBodyService celestialBodyService, HobbyService hobbyService, UserService userService) {
        this.celestialBodyService = celestialBodyService;
        this.hobbyService = hobbyService;
        this.userService = userService;
    }

    @ApiOperation("获取星域")
    @GetMapping("/starDomain")
    public List<CelestialBodyInfo> getStarDomain(double minX, double maxX, double minY, double maxY, String watchUserId) {
        return celestialBodyService.getStarDomain(minX, maxX, minY, maxY, watchUserId);
    }

    @ApiOperation("获取星球详情")
    @GetMapping("/{id}")
    public CelestialBodyInfo getCelestialBody(@PathVariable String id) {
        return celestialBodyService.getCelestialBody(id);
    }

    @ApiOperation("获取星球图片ossKey")
    @GetMapping("/{id}/image")
    public CelestialBodyImageOssKey getCelestialBodyImageOssKey(@PathVariable String id) {
        return celestialBodyService.getCelestialBodyImageOssKey(id);
    }

    @ApiOperation("获取用户的星球")
    @GetMapping("/{userId}/user")
    public CelestialBodyInfo getCelestialBodyByUser(@PathVariable String userId) {
        User user = userService.getUser(userId);
        CelestialBody celestialBody = celestialBodyService.getCelestialBodyByUser(userId);
        return new CelestialBodyInfo(celestialBody).userInfo(user);
    }

    @ApiOperation("获取爱好的星球")
    @GetMapping("/{hobbyId}/hobby")
    public CelestialBodyInfo getCelestialBodyByHobby(@PathVariable String hobbyId) {
        Hobby hobby = hobbyService.getHobby(hobbyId);
        CelestialBody celestialBody = celestialBodyService.getCelestialBodyByHobby(hobbyId);
        return new CelestialBodyInfo(celestialBody).hobbyInfo(hobby);
    }

    @ApiOperation("创建新星球")
    @PostMapping
    public IdBaseResponse saveCelestialBody(@RequestBody SaveCelestialBodyRequest request) {
        String id = celestialBodyService.createCelestialBody(request.getType(), request.getId()).getId();
        return new IdBaseResponse(id);
    }

    @ApiOperation("获取用户星球的爱好列表")
    @GetMapping("/{id}/user/hobby")
    public List<HobbyInfo> geUserCelestialBodyHobbies(@PathVariable String id,
                                                      @RequestParam(required = false, defaultValue = "") String watchUserId) {
        return celestialBodyService.getUserCelestialBodyHobbies(id, watchUserId);
    }

    @ApiOperation("同步用户积分")
    @PostMapping("/user/{userId}/point/sync")
    public RefreshUserIntegralInfo syncUserPoint(@PathVariable String userId) {
        return celestialBodyService.syncUserPoint(userId);
    }
}
