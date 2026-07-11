package com.deundeun.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TODO: 임시 설정. 실제 로그인(OAuth2 + 세션) 붙이는 인증 담당자가 이 클래스를 교체해야 함.
 * 지금은 Spring Security 기본 설정(HTTP Basic, 전체 요청 차단)이 개발을 막고 있어서
 * 우선 모든 요청을 permitAll로 열어둔다. 로그인 여부 확인은 각 컨트롤러가
 * SessionUtils로 세션을 직접 확인하는 방식으로 임시 대체한다.
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
