package com.wejuai.core.config;

import com.wejuai.core.support.WxServiceClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

/**
 * @author ZM.Wang
 */
@Configuration
@EnableConfigurationProperties({WxServiceConfig.Properties.class, WxServiceConfig.Msg.class, WxServiceConfig.Page.class})
public class WxServiceConfig {

    private final Properties weixin;
    private final Msg msg;
    private final Page page;

    public WxServiceConfig(Properties weixin, Msg msg, Page page) {
        this.weixin = weixin;
        this.msg = msg;
        this.page = page;
    }

    @Bean
    WxServiceClient wxServiceClient() {
        return new WxServiceClient(weixin.getGateway());
    }

    @Validated
    @ConfigurationProperties(prefix = "weixin.page")
    public static class Page {
        @NotBlank
        private String article;
        @NotBlank
        private String reward;
        @NotBlank
        private String orders;

        public String getArticle() {
            return article;
        }

        public Page setArticle(String article) {
            this.article = article;
            return this;
        }

        public String getReward() {
            return reward;
        }

        public Page setReward(String reward) {
            this.reward = reward;
            return this;
        }

        public String getOrders() {
            return orders;
        }

        public Page setOrders(String orders) {
            this.orders = orders;
            return this;
        }
    }

    @Validated
    @ConfigurationProperties(prefix = "weixin.msg")
    public static class Msg {
        /** 提现审核通知 */
        @NotBlank
        private String appAudit;

        /** 回复消息提醒通知 */
        @NotBlank
        private String appReply;

        /** 悬赏被选定通知 */
        @NotBlank
        private String appRewardDemandSelected;

        public String getAppAudit() {
            return appAudit;
        }

        public Msg setAppAudit(String appAudit) {
            this.appAudit = appAudit;
            return this;
        }

        public String getAppReply() {
            return appReply;
        }

        public Msg setAppReply(String appReply) {
            this.appReply = appReply;
            return this;
        }

        public String getAppRewardDemandSelected() {
            return appRewardDemandSelected;
        }

        public Msg setAppRewardDemandSelected(String appRewardDemandSelected) {
            this.appRewardDemandSelected = appRewardDemandSelected;
            return this;
        }
    }

    @Validated
    @ConfigurationProperties(prefix = "weixin")
    public static class Properties {
        @NotBlank
        private String gateway;

        public String getGateway() {
            return gateway;
        }

        public Properties setGateway(String gateway) {
            this.gateway = gateway;
            return this;
        }
    }


    public Properties getWeixin() {
        return weixin;
    }

    public Msg getMsg() {
        return msg;
    }

    public Page getPage() {
        return page;
    }


}
