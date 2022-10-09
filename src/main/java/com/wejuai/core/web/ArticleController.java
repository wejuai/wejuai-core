package com.wejuai.core.web;

import com.wejuai.core.service.ArticleService;
import com.wejuai.dto.request.AppAddTextRequest;
import com.wejuai.dto.request.ArticleRevokeRequest;
import com.wejuai.dto.request.SaveArticleDraftRequest;
import com.wejuai.dto.request.SaveArticleRequest;
import com.wejuai.dto.request.SortType;
import com.wejuai.dto.response.ArticleInfo;
import com.wejuai.dto.response.ArticleListInfo;
import com.wejuai.dto.response.IdBaseResponse;
import com.wejuai.dto.response.KeyValue;
import com.wejuai.dto.response.ShareArticleInfo;
import com.wejuai.dto.response.Slice;
import com.wejuai.entity.mysql.ArticleDraft;
import com.wejuai.entity.mysql.GiveType;
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

import java.util.Map;

/**
 * Created by ZM.Wang
 */
@Api(tags = "文章相关")
@RestController
@RequestMapping("/app/article")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @ApiOperation("创建文章")
    @PostMapping
    public IdBaseResponse createArticle(@RequestParam(required = false, defaultValue = "") String id,
                                        @RequestBody SaveArticleRequest request) {
        return articleService.saveArticle(id, request);
    }

    @ApiOperation("添加内容")
    @PutMapping("/addText")
    public void addText(@RequestParam String userId, @RequestBody AppAddTextRequest request) {
        articleService.addText(userId, request);
    }

    @ApiOperation("修改积分")
    @PutMapping("/{id}/integral")
    public void updateIntegral(@PathVariable String id, @RequestParam String userId, @RequestParam long integral) {
        articleService.updateIntegral(id, userId, integral);
    }

    @ApiOperation("获取文章")
    @GetMapping("/{id}")
    public ArticleInfo getArticle(@PathVariable String id, @RequestParam(required = false, defaultValue = "") String watchUserId) {
        return articleService.getArticleInfo(id, watchUserId);
    }

    @ApiOperation("购买文章")
    @GetMapping("/{id}/buy")
    public void buyArticle(@PathVariable String id, @RequestParam String userId) {
        articleService.buyArticle(id, userId);
    }

    @ApiOperation("删除文章")
    @DeleteMapping("/{id}")
    public void deleteArticle(@PathVariable String id,
                              @RequestParam(required = false, defaultValue = "") String userId,
                              @RequestParam(required = false, defaultValue = "") String reason,
                              @RequestParam(required = false, defaultValue = "false") boolean console) {
        articleService.deleteArticle(id, userId, console, reason);
    }

    @ApiOperation("根据条件获取文章列表")
    @GetMapping
    public Page<ArticleListInfo> getArticles(@RequestParam(required = false, defaultValue = "") String userId,
                                             @RequestParam(required = false, defaultValue = "") String watchUserId,
                                             @RequestParam(required = false, defaultValue = "") String titleStr,
                                             @RequestParam(required = false, defaultValue = "") String hobby,
                                             @RequestParam(required = false, defaultValue = "0") int page,
                                             @RequestParam(required = false, defaultValue = "10") int size,
                                             @RequestParam(required = false, defaultValue = "DESC") Sort.Direction direction,
                                             @RequestParam(required = false, defaultValue = "TIME") SortType sortType) {
        Pageable pageable = PageRequest.of(page, size, direction, sortType.getValue());
        return articleService.getArticles(titleStr, hobby, userId, watchUserId, pageable);
    }

    @ApiOperation("根据条件获取文章草稿列表")
    @GetMapping("/articleDraft")
    public Page<ArticleListInfo> getArticleDraftsByAccounts(@RequestParam(required = false, defaultValue = "") String userId,
                                                            @RequestParam(required = false, defaultValue = "") String hobbyId,
                                                            @RequestParam(required = false, defaultValue = "0") int page,
                                                            @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return articleService.getArticleDrafts(hobbyId, userId, pageable);
    }

    @ApiOperation("根据give获取个人文章收藏")
    @GetMapping("/{userId}/give")
    public Slice<ArticleListInfo> getArticlesByGiveType(@PathVariable String userId,
                                                        @RequestParam GiveType giveType,
                                                        @RequestParam(required = false, defaultValue = "0") int page,
                                                        @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return articleService.findArticlesByGiveType(userId, giveType, pageable);
    }

    @ApiOperation("创建文章草稿")
    @PostMapping("/articleDraft")
    public KeyValue saveArticleDraft(@RequestBody SaveArticleDraftRequest request) {
        String id = articleService.saveArticleDraft(request).getId();
        return new KeyValue("id", id);
    }

    @ApiOperation("获取单个草稿")
    @GetMapping("/articleDraft/{id}")
    public ArticleInfo getArticleDraft(@PathVariable String id) {
        ArticleDraft articleDraft = articleService.getArticleDraft(id);
        return new ArticleInfo(articleDraft);
    }

    @ApiOperation("发布文章（草稿变正式）")
    @PostMapping("/publish/{id}")
    public void publishArticle(@PathVariable String id, @RequestParam String userId) {
        articleService.publishArticle(id, userId);
    }

    @ApiOperation("删除文章草稿")
    @DeleteMapping("/articleDraft/{id}")
    public void deleteArticleDraft(@PathVariable String id, @RequestParam String userId) {
        articleService.deleteArticleDraft(id, userId);
    }

    /**
     * 目前pc那用的分享出一个隐私信息页面
     */
    @Deprecated
    @ApiOperation("获取文章分享内容")
    @GetMapping("/share/{id}")
    public ShareArticleInfo getShareInfo(@PathVariable String id) {
        return articleService.getShareInfo(id);
    }

    @ApiOperation("撤销文章发布")
    @PostMapping("/{id}/revoke")
    public void revoke(@PathVariable String id, @RequestBody ArticleRevokeRequest request) {
        articleService.revoke(id, request);
    }
}
