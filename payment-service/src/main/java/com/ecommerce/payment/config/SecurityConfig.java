package com.ecommerce.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: actuator health check, IPN webhook từ VNPAY, Return callback
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                        .requestMatchers("/api/v1/payments/vnpay-return/**").permitAll()
                        .requestMatchers("/api/v1/payments/**").permitAll() // Được Gateway xác thực qua AuthenticationFilter
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
