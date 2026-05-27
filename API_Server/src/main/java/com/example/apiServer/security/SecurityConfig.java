package com.example.apiServer.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
        private final JwtAuthFilter jwtAuthFilter;

        public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
                this.jwtAuthFilter = jwtAuthFilter;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                // ✅ cho phép login không cần token
                                                .requestMatchers("/api/dynamic/login", "/PYS Images/**",
                                                                "/api/telegram/**",
                                                                "/api/upload-guarantee/**",
                                                                "/api/upload/**",
                                                                "/ws/**",
                                                                "/update/**",
                                                                "/api/update/**",
                                                                "/api/dynamic/version-warehouse",
                                                                "/api/business/get-all")
                                                .permitAll()
                                                // 🔒 còn lại cần token
                                                .anyRequest().authenticated())
                                .addFilterBefore(
                                                jwtAuthFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
