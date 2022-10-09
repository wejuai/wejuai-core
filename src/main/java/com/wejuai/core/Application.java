package com.wejuai.core;

import com.wejuai.core.config.WejuaiBanner;
import com.wejuai.entity.mysql.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;

@EntityScan(basePackageClasses = {User.class})
@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        app.setBanner(new WejuaiBanner());
        Environment env = app.run(args).getEnvironment();
        String port = env.getProperty("server.port");
        logger.info("\nAccess URLs:\n----------------------------------------------------------\n"
                + "Local: \t\thttp://127.0.0.1:{}/swagger-ui/index.html\n" +
                "----------------------------------------------------------", port);
    }

}
