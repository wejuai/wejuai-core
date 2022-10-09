package com.wejuai.core.support;

import com.endofmaster.rest.exception.BadRequestException;
import com.endofmaster.rest.exception.ServerException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author ZM.Wang
 */
public class WxServiceClient {

    private final RestTemplate restTemplate;
    private final String url;

    public WxServiceClient(String url) {
        this.url = url;
        this.restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().clear();
        ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter(MAPPER));
        List<ClientHttpRequestInterceptor> interceptors = Arrays.asList(
                new HeaderRequestInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE),
                new HeaderRequestInterceptor("Content-Type", MediaType.APPLICATION_JSON_VALUE));
        this.restTemplate.setInterceptors(interceptors);
    }

    public void sendWxTemplateMsg(WxTemplateMsg msg) {
        try {
            restTemplate.postForObject(url + "/message/subscribe", msg, Void.class);
        } catch (HttpServerErrorException e) {
            throw new ServerException(e.getResponseBodyAsString());
        } catch (HttpClientErrorException e) {
            throw new BadRequestException(e.getResponseBodyAsString());
        }
    }

    public static class HeaderRequestInterceptor implements ClientHttpRequestInterceptor {

        private final String headerName;
        private final String headerValue;

        public HeaderRequestInterceptor(String headerName, String headerValue) {
            this.headerName = headerName;
            this.headerValue = headerValue;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            wrapper.getHeaders().set(headerName, headerValue);
            return execution.execute(wrapper, body);
        }
    }
}
