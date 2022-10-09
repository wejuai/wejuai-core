package com.wejuai.core.service;

import com.wejuai.core.repository.mongo.UserPointRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static com.wejuai.core.config.Constant.FREE_ITEM_INTEGRAL_UPPER_LIMIT;

@SpringBootTest(properties = "spring.profiles.active=dev")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserPointRecordRepository userPointRecordRepository;

    @Test
    public void getUserPointByDay() {
        long asdadas = userService.getUserPointByDay("5f682ef88fa24d0001647dbd", LocalDate.now(), FREE_ITEM_INTEGRAL_UPPER_LIMIT);
        System.err.println(asdadas);
    }

    @Test
    public void saveUserPointRecord() {
        boolean b = userService.hasUserTodayDailyLogin("5f682ef88fa24d0001647dbe");
        System.err.println(b);
    }

}