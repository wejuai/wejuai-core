package com.wejuai.core.web;

import com.wejuai.core.service.CommentService;
import com.wejuai.dto.request.CommentType;
import com.wejuai.dto.request.SaveCommentRequest;
import com.wejuai.dto.request.SaveRemindsRequest;
import com.wejuai.dto.request.SaveSubCommentRequest;
import com.wejuai.dto.response.CommentInfo;
import com.wejuai.dto.response.RemindInfo;
import com.wejuai.dto.response.Slice;
import com.wejuai.dto.response.SubCommentInfo;
import com.wejuai.dto.response.SystemMessageInfo;
import com.wejuai.entity.mongo.AppType;
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

/**
 * @author ZM.Wang
 */
@Api(tags = "评论功能")
@RestController
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @ApiOperation("创建一级评论")
    @PostMapping
    public void createComment(@RequestBody SaveCommentRequest request) {
        commentService.createComment(request);
    }

    @ApiOperation("创建二级评论")
    @PostMapping("/sub")
    public void createSubComment(@RequestBody SaveSubCommentRequest request) {
        commentService.createSubComment(request);
    }

    @ApiOperation("创建艾特信息")
    @PostMapping("/remind")
    public void createReminds(@RequestBody SaveRemindsRequest request) {
        commentService.createReminds(request);
    }

    @ApiOperation("删除一级评论")
    @DeleteMapping("/{id}")
    public void removeComment(@PathVariable String id, @RequestParam String userId) {
        commentService.removeComment(id, userId);
    }

    @ApiOperation("删除二级评论")
    @DeleteMapping("/{id}/sub")
    public void removeSubComment(@PathVariable String id, @RequestParam String userId) {
        commentService.removeSubComment(id, userId);
    }

    @ApiOperation("获取一级评论分页")
    @GetMapping
    public Slice<CommentInfo> getComments(@RequestParam(required = false, defaultValue = "") AppType appType,
                                          @RequestParam(required = false, defaultValue = "") String appId,
                                          @RequestParam(required = false, defaultValue = "") String userId,
                                          @RequestParam(required = false, defaultValue = "") String watchUserId,
                                          @RequestParam(required = false, defaultValue = "0") long page,
                                          @RequestParam(required = false, defaultValue = "10") long size) {
        return commentService.getComments(appType, appId, userId, watchUserId, page, size);
    }

    @ApiOperation("获取二级评论分页")
    @GetMapping("/sub")
    public Slice<SubCommentInfo> getSubComments(@RequestParam(required = false, defaultValue = "") String commentId,
                                                @RequestParam(required = false, defaultValue = "") String userId,
                                                @RequestParam(required = false, defaultValue = "") String watchUserId,
                                                @RequestParam(required = false, defaultValue = "0") long page,
                                                @RequestParam(required = false, defaultValue = "10") long size) {
        return commentService.getSubComments(commentId, userId, watchUserId, page, size);
    }

    @ApiOperation("获取某用户艾特分页")
    @GetMapping("/remind")
    public Page<RemindInfo> getReminds(@RequestParam String recipient,
                                       @RequestParam(required = false, defaultValue = "") int page,
                                       @RequestParam(required = false, defaultValue = "") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return commentService.getReminds(recipient, pageable);
    }

    @ApiOperation("阅读评论")
    @PutMapping("/watch/{id}")
    public void watch(@PathVariable String id, @RequestParam String userId, @RequestParam CommentType type) {
        commentService.watch(type, userId, id);
    }

    @ApiOperation("系统消息列表")
    @GetMapping("/systemMessage")
    public Page<SystemMessageInfo> getSystemMessages(@RequestParam String userId,
                                                     @RequestParam(required = false, defaultValue = "0") int page,
                                                     @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return commentService.getSystemMessages(userId, pageable);
    }

}
