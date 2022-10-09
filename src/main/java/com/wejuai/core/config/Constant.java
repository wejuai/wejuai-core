package com.wejuai.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Random;

public interface Constant {

    String CELESTIAL_BODY_STATISTICS_ID = "CelestialBodyStatistics";
    String ORDERS_STATISTICS_ID = "OrdersStatistics";
    String CHARGE_STATISTICS_ID = "ChargeStatistics";

    Random RANDOM = new Random();

    long PAID_ITEM_INTEGRAL_UPPER_LIMIT = 500;

    long FREE_ITEM_INTEGRAL_UPPER_LIMIT = 50;

    ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

}
