package com.wejuai.core.service;

import com.wejuai.core.config.WxServiceConfig;
import com.wejuai.core.support.WxServiceClient;
import com.wejuai.core.support.WxTemplateMsg;
import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mongo.Comment;
import com.wejuai.entity.mongo.Remind;
import com.wejuai.entity.mongo.Reply;
import com.wejuai.entity.mongo.SubComment;
import com.wejuai.entity.mysql.RewardDemand;
import com.wejuai.entity.mysql.RewardSubmission;
import com.wejuai.entity.mysql.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author ZM.Wang
 */
@Service
public class WxMessageService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserService userService;

    private final WxServiceClient wxServiceClient;
    private final WxServiceConfig.Msg wxMsg;
    private final WxServiceConfig.Page page;

    public WxMessageService(UserService userService, WxServiceClient wxServiceClient, WxServiceConfig.Msg wxMsg, WxServiceConfig.Page page) {
        this.userService = userService;
        this.wxServiceClient = wxServiceClient;
        this.wxMsg = wxMsg;
        this.page = page;
    }

    /**
     * 发送回复消息
     *
     * @param reply 回复体
     */
    public void sendReplyMsg(Reply reply) {
        WxTemplateMsg msg = buildReplyMsg(reply);

        User appCreator = userService.getUser(reply.getAppCreator());
        if (reply instanceof Comment) {
            sendReplyMsg(appCreator, msg);
        }
        if (reply instanceof SubComment) {
            SubComment subComment = (SubComment) reply;
            User recipient = userService.getUser(subComment.getRecipient());
            sendReplyMsg(appCreator, msg);
            sendReplyMsg(recipient, msg);
        }
        if (reply instanceof Remind) {
            Remind remind = (Remind) reply;
            User recipient = userService.getUser(remind.getRecipient());
            sendReplyMsg(recipient, msg);
        }
    }

    public void batchSendRely(List<User> users, Reply reply) {
        WxTemplateMsg msg = buildReplyMsg(reply);
        users.forEach(user -> sendReplyMsg(user, msg));
    }

    public void sendRewardDemandSelectedResultMsg(RewardSubmission rewardSubmission) {
        User recipient = rewardSubmission.getUser();
        RewardDemand rewardDemand = rewardSubmission.getRewardDemand();
        String title = rewardDemand.getTitle();
        long integral = rewardDemand.getIntegral();
        String url = page.getReward() + "?id=" + rewardDemand.getId();
        WxTemplateMsg msg = new WxTemplateMsg().setTemplateId(wxMsg.getAppRewardDemandSelected()).setUrl(url)
                .addDataItem("thing1", title)
                .addDataItem("thing2", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")))
                .addDataItem("number3", integral + "");
        sendReplyMsg(recipient, msg);
    }

    private WxTemplateMsg buildReplyMsg(Reply reply) {
        User sender = userService.getUser(reply.getSender());
        String title = reply.getAppTitle().length() > 14 ? reply.getAppTitle().substring(0, 13) + "..." : reply.getAppTitle();
        String content = reply.getText().length() > 14 ? reply.getText().substring(0, 13) + "..." : reply.getText();
        String url;
        if (reply.getAppType() == AppType.ARTICLE) {
            url = page.getArticle();
        } else if (reply.getAppType() == AppType.REWARD_DEMAND) {
            url = page.getReward();
        } else {
            throw new RuntimeException("没有该评论类型: " + reply.getAppType());
        }
        url += "?id=" + reply.getAppId();
        return new WxTemplateMsg().setTemplateId(wxMsg.getAppReply()).setUrl(url)
                .addDataItem("thing1", title).addDataItem("thing2", content)
                .addDataItem("date3", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")))
                .addDataItem("thing7", sender.getNickName());
    }

    private void sendReplyMsg(User user, WxTemplateMsg msg) {
        String openId = user.getAccounts().getWeixinUser().getAppOpenId();
        if (StringUtils.isBlank(openId)) {
            logger.warn("发送回复模版消息, 该用户未绑定小程序openId: " + user.getId());
            return;
        }
        wxServiceClient.sendWxTemplateMsg(msg.setOpenId(openId));
    }

}
