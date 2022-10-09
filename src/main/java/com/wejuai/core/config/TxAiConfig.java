package com.wejuai.core.config;

import com.endofmaster.txAi.TxAiClient;
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
@EnableConfigurationProperties(TxAiConfig.Properties.class)
public class TxAiConfig {

    public final Properties txAi;

    public TxAiConfig(Properties txAi) {
        this.txAi = txAi;
    }

    @Bean
    TxAiClient txAiClient() {
        return new TxAiClient(txAi.getAppId(), txAi.getAppKey());
    }

    @Validated
    @ConfigurationProperties(prefix = "tx-ai")
    public static class Properties {

        @NotBlank
        private String appId;
        @NotBlank
        private String appKey;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }
    }
}
