package com.wejuai.core.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ZM.Wang
 */
@RestController
@RequestMapping("/test")
public class TestController {

    private final static Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/{origin}")
    public void test(@PathVariable String origin, HttpServletRequest request) {
        logger.debug("项目启动来源测试: " + origin);
        logger.debug("RemoteAddr: " + request.getRemoteAddr());
        logger.debug("LocalAddr: " + request.getLocalAddr());
        logger.debug("ContextPath: " + request.getContextPath());
    }

    /** 阿里云就绪检查 */
    @GetMapping
    public void ackHeathCheck() {
    }
}
