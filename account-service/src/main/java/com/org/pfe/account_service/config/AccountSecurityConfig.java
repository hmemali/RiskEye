package com.org.pfe.account_service.config;

import com.org.pfe.account_service.security.AccountJwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class AccountSecurityConfig {

    private final AccountJwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain accountSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/me/accounts/**").hasAnyRole("CLIENT", "USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/accounts/owner/**", "/api/v1/accounts/owner/**")
                            .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/accounts/*", "/api/v1/accounts/*")
                            .permitAll()
                        .requestMatchers("/api/accounts/**", "/api/v1/accounts/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}