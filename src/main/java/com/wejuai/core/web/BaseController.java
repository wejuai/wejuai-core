package com.wejuai.core.web;

import com.wejuai.core.service.AppBaseService;
import com.wejuai.dto.response.KeyValue;
import com.wejuai.dto.response.UserInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ZM.Wang
 */
@Api(tags = "基础接口")
@RestController
@RequestMapping("/base")
public class BaseController {

    private final AppBaseService appBaseService;

    public BaseController(AppBaseService appBaseService) {
        this.appBaseService = appBaseService;
    }

    @ApiOperation("获取user公共信息")
    @GetMapping("/user/{id}")
    public UserInfo getUser(@PathVariable String id,
                            @RequestParam(required = false, defaultValue = "") String watchUserId) {
        return appBaseService.getOtherUser(id, watchUserId);
    }

    @ApiOperation("模糊查询用户列表")
    @GetMapping("/user/nickName")
    public Page<KeyValue> getUsersByNickName(@RequestParam(required = false, defaultValue = "") String chars,
                                             @RequestParam(required = false, defaultValue = "0") int page,
                                             @RequestParam(required = false, defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return appBaseService.getUsersByNickName(chars, pageable);
    }

}
