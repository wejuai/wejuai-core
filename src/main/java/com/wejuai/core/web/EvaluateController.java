package com.wejuai.core.web;

import com.endofmaster.rest.exception.BadRequestException;
import com.wejuai.core.service.EvaluateService;
import com.wejuai.dto.request.SaveEvaluateRequest;
import com.wejuai.dto.response.EvaluateInfo;
import com.wejuai.entity.mongo.AppType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ZM.Wang
 */
@Api(tags = "评价相关")
@RestController
@RequestMapping("/evaluate")
public class EvaluateController {

    private final EvaluateService evaluateService;

    public EvaluateController(EvaluateService evaluateService) {
        this.evaluateService = evaluateService;
    }

    @ApiOperation("评价")
    @PostMapping
    public void evaluate(@RequestBody SaveEvaluateRequest request) {
        if (request.getAppType() == AppType.ARTICLE) {
            evaluateService.articleEvaluate(request);
        } else if (request.getAppType() == AppType.REWARD_DEMAND) {
            evaluateService.rewardDemandEvaluate(request);
        } else {
            throw new BadRequestException("没有该类型评价");
        }
    }

    @ApiOperation("获取文章评价分页")
    @GetMapping
    public Page<EvaluateInfo> getEvaluates(@RequestParam String appId,
                                           @RequestParam(required = false, defaultValue = "0") int page,
                                           @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return evaluateService.getEvaluates(appId, pageable);
    }
}
