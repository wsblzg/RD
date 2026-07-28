package com.example.controller.collectible;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PointsControllerRouteTest {

    @Test
    void exposesPointsApiThroughExistingApiGatewayPrefix() {
        RequestMapping mapping = PointsController.class.getAnnotation(RequestMapping.class);

        assertTrue(
                Arrays.asList(mapping.value()).contains("/api/points"),
                "积分接口必须同时暴露 /api/points，确保线上现有 /api 反向代理可以访问"
        );
    }
}
