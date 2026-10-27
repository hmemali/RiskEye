package com.org.pfe.transaction_service.config;

import com.org.pfe.transaction_service.security.TransactionJwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class TransactionSecurityConfig {

    private final TransactionJwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain transactionSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/transactions/suspicious", "/api/v1/transactions/suspicious").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/transactions", "/api/transactions/account/**",
                                "/api/v1/transactions", "/api/v1/transactions/account/**").hasRole("ADMIN")
                        .requestMatchers("/api/transactions/me", "/api/v1/transactions/me", "/api/v1/me/transactions")
                        .hasAnyRole("CLIENT", "USER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}