package com.deundeun.global.config;

import com.deundeun.global.security.handler.CustomAuthenticationEntryPoint;
import com.deundeun.global.security.jwt.JwtAuthenticationFilter;
import com.deundeun.global.security.jwt.JwtProvider;
import com.deundeun.global.security.oauth2.CustomOAuth2UserService;
import com.deundeun.global.security.oauth2.CustomOidcUserService;
import com.deundeun.global.security.oauth2.OAuth2FailureHandler;
import com.deundeun.global.security.oauth2.OAuth2SuccessHandler;
import com.deundeun.global.security.oauth2.ReauthAwareAuthorizationRequestResolver;
import com.deundeun.global.security.oauth2.RedisOAuth2AuthorizationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final ClientRegistrationRepository clientRegistrationRepository;

    private static final String AUTHORIZATION_BASE_URI = "/api/oauth2/authorization";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(a -> a
                                .baseUri(AUTHORIZATION_BASE_URI)
                                .authorizationRequestResolver(new ReauthAwareAuthorizationRequestResolver(
                                        clientRegistrationRepository, AUTHORIZATION_BASE_URI))
                                .authorizationRequestRepository(authorizationRequestRepository))
                        .redirectionEndpoint(r -> r.baseUri("/api/login/oauth2/code/*"))
                        .userInfoEndpoint(u -> u
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .exceptionHandling(e -> e.authenticationEntryPoint(customAuthenticationEntryPoint))
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
