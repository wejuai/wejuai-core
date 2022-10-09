package com.wejuai.core.web;

import com.wejuai.core.service.AppBaseService;
import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mysql.GiveType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

/**
 * @author ZM.Wang
 */
@Api(tags = "应用相关基础功能")
@RestController
@RequestMapping("/app")
public class AppBaseController {

    private final AppBaseService appBaseService;

    public AppBaseController(AppBaseService appBaseService) {
        this.appBaseService = appBaseService;
    }

    @ApiOperation("添加应用的各项参数")
    @PutMapping("/give/{id}")
    public void giveNum(@PathVariable String id, @RequestParam String userId, @RequestParam GiveType type, @RequestParam AppType appType) {
        appBaseService.giveNum(appType, id, userId, type);
    }

    @ApiOperation("减少应用的各项参数")
    @PutMapping("/reduce/{id}")
    public void reduceNum(@PathVariable String id, @RequestParam String userId, @RequestParam GiveType type, @RequestParam AppType appType) {
        appBaseService.reduceNum(appType, id, userId, type);
    }

}
