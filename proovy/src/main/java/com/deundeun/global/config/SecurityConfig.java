package com.deundeun.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TODO: 임시 설정. 실제 로그인(OAuth2 + 세션) 붙이는 인증 담당자(@장인호)가 이 클래스를 교체해야 함.
 * 지금은 Spring Security 기본 설정(HTTP Basic, 전체 요청 차단)이 개발을 막고 있어서
 * 우선 모든 요청을 permitAll로 열어둔다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
