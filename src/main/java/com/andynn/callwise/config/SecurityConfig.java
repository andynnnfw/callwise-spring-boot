package com.andynn.callwise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/totem/**", "/painel/**", "/ws/**", "/login/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/senhas").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login/index.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/atendente/index.html", true)
                        .failureUrl("/login/index.html?erro"))
                .logout(logout -> logout
                        .logoutSuccessUrl("/login/index.html?saiu"));

        return http.build();
    }
}
