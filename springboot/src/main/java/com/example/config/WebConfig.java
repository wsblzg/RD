// WebConfig.java
package com.example.config;

import com.example.interceptor.JwtInterceptor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    @Lazy
    private JwtInterceptor jwtInterceptor;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置Jackson
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new JavaTimeModule()); // 注册Java8时间模块
        return objectMapper;
    }

    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        return converter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/collectibles/**",
                    "/admin/collectibles/**",
                    "/api/collectibles/**",
                    "/api/admin/collectibles/**",
                    "/shop/products",
                    "/shop/products/**",
                    "/shop/payment-config",
                    "/api/shop/products",
                    "/api/shop/products/**",
                    "/api/shop/payment-config",
                    "/project-media/**",
                    "/api/ai/**",
                    "/api/ceramic-creation/**",
                    "/",
                    "/home",
                    "/new-home",
                    "/user-login",
                    "/login",
                    "/guide",
                    "/guide/**",
                    "/collections",
                    "/community",
                    "/intelligence",
                    "/intelligence/**",
                    "/ai-creation",
                    "/transformation",
                    "/transformation/**",
                    "/about",
                    "/about/**",
                    "/ceramics",
                    "/ceramics/**",
                    "/assets/**",
                    "/index.html",
                    "/error",
                    "/favicon.ico",
                    "/*.webp",
                    "/*.png",
                    "/*.svg",
                    "/*.mp4"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost",
                        "http://localhost:*",
                        "http://127.0.0.1",
                        "http://127.0.0.1:*",
                        "http://47.113.113.212",
                        "http://47.113.113.212:*",
                        "https://47.113.113.212",
                        "https://47.113.113.212:*",
                        "http://yaochuangfuture.cn",
                        "https://yaochuangfuture.cn",
                        "http://www.yaochuangfuture.cn",
                        "https://www.yaochuangfuture.cn"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(
                        "Authorization",
                        "Content-Disposition",
                        "Access-Control-Allow-Origin",
                        "Access-Control-Allow-Credentials",
                        "Access-Control-Expose-Headers"
                )
                .allowCredentials(true);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 先添加Jackson转换器，保证对象优先被序列化为JSON
        converters.add(mappingJackson2HttpMessageConverter());
        // 再添加String转换器（可选）
        converters.add(stringHttpMessageConverter());
    }
}
