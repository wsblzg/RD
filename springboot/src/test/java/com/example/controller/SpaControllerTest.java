package com.example.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SpaController()).build();
    }

    @Test
    void forwardsCeramicsHistoryRouteToIndex() throws Exception {
        mockMvc.perform(get("/ceramics/community/post/42"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void forwardsLegacyFrontendRouteToIndex() throws Exception {
        mockMvc.perform(get("/about/project"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void doesNotCaptureApiRoute() throws Exception {
        mockMvc.perform(get("/api/not-a-real-route"))
            .andExpect(status().isNotFound());
    }

    @Test
    void doesNotCaptureMissingAsset() throws Exception {
        mockMvc.perform(get("/assets/missing.js"))
            .andExpect(status().isNotFound());
    }

    @Test
    void doesNotCaptureBackendShopRoute() throws Exception {
        mockMvc.perform(get("/shop/cart"))
            .andExpect(status().isNotFound());
    }
}
