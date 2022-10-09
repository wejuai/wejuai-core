package com.wejuai.core.config;

import com.endofmaster.commons.aliyun.oss.AliyunOss;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

/**
 * @author YQ.Huang
 */
@Configuration
@EnableConfigurationProperties({
        AliyunConfig.Properties.class,
        AliyunConfig.OssProperties.class})
public class AliyunConfig {

    public final Properties aliyun;
    public final OssProperties oss;

    public AliyunConfig(Properties aliyun, OssProperties oss) {
        this.aliyun = aliyun;
        this.oss = oss;
    }

    @Bean
    AliyunOss aliyunOss() {
        return new AliyunOss(oss.getBucket(),
                oss.getEndpoint(),
                aliyun.getAccessKeyId(),
                aliyun.getAccessKeySecret());
    }

    @Validated
    @ConfigurationProperties(prefix = "aliyun")
    public static class Properties {

        @NotBlank
        private String accessKeyId;
        @NotBlank
        private String accessKeySecret;

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }
    }

    @Validated
    @ConfigurationProperties(prefix = "aliyun.oss")
    public static class OssProperties {

        @NotBlank
        private String bucket;

        @NotBlank
        private String endpoint = "oss-cn-beijing.aliyuncs.com";

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}