    package com.org.pfe.authen_service.config;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
    import org.springframework.security.config.http.SessionCreationPolicy;
    import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
    import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
    import org.springframework.security.web.SecurityFilterChain;
import com.org.pfe.authen_service.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;

    @Configuration
    @EnableWebSecurity
        @RequiredArgsConstructor
    public class SecurityConfig {

                private final JwtAuthFilter jwtAuthFilter;

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

                @Bean
                public UserDetailsService noLocalPasswordUsers() {
                        return username -> {
                                throw new UsernameNotFoundException("Password authentication is not configured");
                        };
                }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

            http
                    .csrf(csrf -> csrf.disable())

                    .cors(cors -> {})

                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )

                    .httpBasic(httpBasic -> httpBasic.disable())
                    .formLogin(formLogin -> formLogin.disable())

                    .authorizeHttpRequests(auth -> auth

                            // Authentification publique
                            .requestMatchers(
                                    "/api/auth/**",
                                    "/api/internal/users/**",
                                    "/actuator/**"
                            ).permitAll()

                            .requestMatchers("/api/users/**").hasRole("ADMIN")

                            .requestMatchers("/api/v1/me/profile/**").hasAnyRole("CLIENT", "USER", "ADMIN")

                            // Tout le reste
                            .anyRequest().authenticated()
                    );

                        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                        return http.build();
        }
    }