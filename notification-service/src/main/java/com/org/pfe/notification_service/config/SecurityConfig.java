package com.org.pfe.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import com.org.pfe.notification_service.security.NotificationJwtAuthFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final NotificationJwtAuthFilter notificationJwtAuthFilter;

    @Bean
    public UserDetailsService noLocalPasswordUsers() {
        return username -> {
            throw new UsernameNotFoundException("Password authentication is not configured");
        };
    }

    @Bean
    public SecurityFilterChain notificationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/notifications/fraud-alert").permitAll()
                        .requestMatchers("/ws/alerts/**").permitAll()
                    .anyRequest().authenticated()
                )
                .addFilterBefore(notificationJwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}