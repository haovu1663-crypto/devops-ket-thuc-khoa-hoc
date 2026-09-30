package com.ecommerce.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Cho phép các nguồn gốc (Origin) phổ biến từ Frontend / Mobile
        corsConfig.setAllowedOriginPatterns(Collections.singletonList("*"));
        
        // Cho phép credentials (Cookies, Auth Headers)
        corsConfig.setAllowCredentials(true);
        
        // Các HTTP methods cho phép
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        
        // Cho phép tất cả Headers
        corsConfig.setAllowedHeaders(Collections.singletonList("*"));
        
        // Header được trả về cho Client
        corsConfig.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count", "Link"));
        
        // Cache kết quả pre-flight request trong 1 giờ
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
