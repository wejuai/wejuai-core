package com.wejuai.core.web;

import com.wejuai.core.service.RewardDemandService;
import com.wejuai.dto.request.AppAddTextRequest;
import com.wejuai.dto.request.ApplyCancelRewardDemandRequest;
import com.wejuai.dto.request.ArticleRevokeRequest;
import com.wejuai.dto.request.SaveRewardDemandRequest;
import com.wejuai.dto.request.SaveRewardSubmissionDraftRequest;
import com.wejuai.dto.request.SaveRewardSubmissionRequest;
import com.wejuai.dto.request.SortType;
import com.wejuai.dto.response.RewardDemandInfo;
import com.wejuai.dto.response.RewardDemandListInfo;
import com.wejuai.dto.response.RewardSubmissionDraftInfo;
import com.wejuai.dto.response.RewardSubmissionInfo;
import com.wejuai.dto.response.Slice;
import com.wejuai.entity.mysql.GiveType;
import com.wejuai.entity.mysql.RewardDemandStatus;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @author ZM.Wang
 */
@Api(tags = "悬赏功能相关")
@RequestMapping("/app/rewardDemand")
@RestController
public class RewardDemandController {

    private final RewardDemandService rewardDemandService;

    public RewardDemandController(RewardDemandService rewardDemandService) {
        this.rewardDemandService = rewardDemandService;
    }

    @ApiOperation("创建悬赏")
    @PostMapping
    public void saveRewardDemand(@RequestBody @Valid SaveRewardDemandRequest request) {
        rewardDemandService.saveRewardDemand(request);
    }

    @ApiOperation("添加内容")
    @PutMapping("/addText")
    public void addText(@RequestParam String userId, @RequestBody AppAddTextRequest request) {
        rewardDemandService.addText(userId, request);
    }

    @ApiOperation("删除悬赏")
    @DeleteMapping("/{id}")
    public void deleteRewardDemand(@PathVariable String id, @RequestParam String userId) {
        rewardDemandService.deleteRewardDemand(id, userId);
    }

    @ApiOperation("增加悬赏金")
    @PutMapping("/{id}/addReward")
    public void addReward(@PathVariable String id, @RequestParam String userId, @RequestParam long integral) {
        rewardDemandService.addReward(id, userId, integral);
    }

    @ApiOperation("获取悬赏分页列表")
    @GetMapping
    public Page<RewardDemandListInfo> getRewardDemands(@RequestParam(required = false, defaultValue = "") String hobby,
                                                       @RequestParam(required = false, defaultValue = "") String userId,
                                                       @RequestParam(required = false, defaultValue = "") String titleStr,
                                                       @RequestParam(required = false, defaultValue = "") String watchUserId,
                                                       @RequestParam(required = false, defaultValue = "") RewardDemandStatus status,
                                                       @RequestParam(required = false, defaultValue = "10") int page,
                                                       @RequestParam(required = false, defaultValue = "0") int size,
                                                       @RequestParam(required = false, defaultValue = "DESC") Sort.Direction direction,
                                                       @RequestParam(required = false, defaultValue = "TIME") SortType sortType) {
        Pageable pageable = PageRequest.of(page, size, direction, sortType.getValue());
        return rewardDemandService.getRewardDemands(titleStr, hobby, userId, watchUserId, status, pageable);
    }

    @ApiOperation("获取悬赏详细信息")
    @GetMapping("/{id}")
    public RewardDemandInfo getRewardDemandInfo(@PathVariable String id, @RequestParam String watchUserId) {
        return rewardDemandService.getRewardDemandInfo(id, watchUserId);
    }

    @ApiOperation("悬赏回答列表")
    @GetMapping("/{id}/rewardSubmission")
    public Slice<RewardSubmissionInfo> getRewardSubmissions(@PathVariable String id,
                                                            @RequestParam(required = false, defaultValue = "") String watchUserId,
                                                            @RequestParam(required = false, defaultValue = "false") boolean self,
                                                            @RequestParam(required = false, defaultValue = "0") int page,
                                                            @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return rewardDemandService.getRewardSubmissions(id, watchUserId, self, pageable);
    }

    @ApiOperation("根据give获取个人文章收藏")
    @GetMapping("/{userId}/give")
    public Page<RewardDemandListInfo> getRewardDemandsByGiveType(@PathVariable String userId,
                                                                 @RequestParam GiveType giveType,
                                                                 @RequestParam(required = false, defaultValue = "0") int page,
                                                                 @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return rewardDemandService.getRewardDemandsByGiveType(userId, giveType, pageable).map(RewardDemandListInfo::new);
    }

    @ApiOperation("提交悬赏结果")
    @PostMapping("/result")
    public void saveResult(@RequestBody @Valid SaveRewardSubmissionRequest request) {
        rewardDemandService.saveResult(request);
    }

    @ApiOperation("选定结果")
    @PostMapping("/selected/{id}")
    public void selectedResult(@PathVariable String id, @RequestParam String userId) {
        rewardDemandService.selectedResult(id, userId);
    }

    @ApiOperation("延时悬赏")
    @PutMapping("/{id}/extension")
    public void extensionRewardDemand(@PathVariable String id,
                                      @RequestParam(required = false, defaultValue = "") String userId,
                                      @RequestParam(required = false, defaultValue = "false") boolean console) {
        rewardDemandService.extensionRewardDemand(id, userId, console);
    }

    @ApiOperation("申请取消")
    @PostMapping("/applyCancel")
    public void applyCancel(@RequestBody @Valid ApplyCancelRewardDemandRequest request) {
        rewardDemandService.applyCancel(request);
    }

    @ApiOperation("悬赏回答撤回")
    @PostMapping("/rewardSubmission/{id}/revoke")
    public void revoke(@PathVariable String id, @RequestBody ArticleRevokeRequest request) {
        rewardDemandService.revoke(id, request);
    }

    @ApiOperation("悬赏回答草稿列表")
    @GetMapping("/rewardSubmission/draft")
    public Slice<RewardSubmissionDraftInfo> getRewardSubmissionDrafts(@RequestParam(required = false, defaultValue = "") String userId,
                                                                      @RequestParam(required = false, defaultValue = "0") int page,
                                                                      @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return rewardDemandService.getRewardSubmissionDrafts(userId, pageable);
    }

    @ApiOperation("悬赏回答草稿详情")
    @GetMapping("/rewardSubmission/draft/{draftId}")
    public RewardSubmissionDraftInfo draftDetails(@PathVariable String draftId, @RequestParam String userId) {
        return rewardDemandService.rewardSubmissionDraftDetails(draftId, userId);
    }

    @ApiOperation("修改悬赏草稿")
    @PutMapping("/rewardSubmission/draft")
    public void saveRewardSubmissionDraft(@RequestBody @Valid SaveRewardSubmissionDraftRequest request) {
        rewardDemandService.saveRewardSubmissionDraft(request);
    }

    @ApiOperation("发布草稿为回答")
    @PostMapping("/rewardSubmission/draft/{draftId}/publish")
    public void publishRewardSubmission(@PathVariable String draftId, @RequestParam String userId) {
        rewardDemandService.publishRewardSubmission(draftId, userId);
    }

    @ApiOperation("删除悬赏回答草稿")
    @DeleteMapping("/rewardSubmission/draft/{draftId}")
    public void delRewardSubmissionDraft(@PathVariable String draftId, @RequestParam String userId) {
        rewardDemandService.delRewardSubmissionDraft(draftId, userId);
    }

}
