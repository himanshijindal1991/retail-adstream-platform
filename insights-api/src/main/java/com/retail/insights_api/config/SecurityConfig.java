package com.retail.insights_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Stateless read-only demo endpoints
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/ad/**").authenticated()
                .anyRequest().denyAll())
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${demo.security.target-password}") String targetPassword,
            @Value("${demo.security.walmart-password}") String walmartPassword,
            PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("target-user").password(encoder.encode(targetPassword)).roles("T001").build(),
            User.withUsername("walmart-user").password(encoder.encode(walmartPassword)).roles("T002").build()
        );
    }
}
