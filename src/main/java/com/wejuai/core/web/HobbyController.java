package com.wejuai.core.web;

import com.wejuai.core.service.HobbyService;
import com.wejuai.dto.response.GetHobbyDomain;
import com.wejuai.dto.response.HobbyInfo;
import com.wejuai.entity.mysql.Hobby;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ZM.Wang
 */
@Api(tags = "爱好相关")
@RestController
@RequestMapping("/hobby")
public class HobbyController {

    private final HobbyService hobbyService;

    public HobbyController(HobbyService hobbyService) {
        this.hobbyService = hobbyService;
    }

    @ApiOperation("根据id获取爱好名称")
    @GetMapping("/{id}/name")
    public HobbyInfo getHobbyName(@PathVariable String id) {
        Hobby hobby = hobbyService.getHobby(id);
        return new HobbyInfo(hobby);
    }

    @ApiOperation("获取用户的爱好列表")
    @GetMapping("/{userId}/user")
    public List<HobbyInfo> getHobbies(@PathVariable String userId, @RequestParam String watchUserId) {
        return hobbyService.getHobbies(userId, watchUserId).stream().map(HobbyInfo::new).collect(Collectors.toList());
    }

    @ApiOperation("用户关注爱好")
    @PostMapping("/follow/{id}")
    public void followHobby(@RequestParam String userId, @PathVariable String id) {
        hobbyService.followHobby(userId, id);
    }

    @ApiOperation("用户取消关注爱好")
    @PostMapping("/unfollow/{id}")
    public void unfollowHobby(@RequestParam String userId, @PathVariable String id) {
        hobbyService.unfollowHobby(userId, id);
    }

    @ApiOperation("搜索爱好")
    @GetMapping("/tab")
    public GetHobbyDomain getHobbyDomainByTab(String userId, String tab) {
        return hobbyService.getHobbyDomainByTab(userId, tab);
    }
}
