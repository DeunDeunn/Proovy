package com.deundeun.global.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
public class RedisOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String KEY_PREFIX = "OAUTH_STATE:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String STATE_PARAM_NAME = "state";

    private final RedisTemplate<String, OAuth2AuthorizationRequest> redisTemplate;

    public RedisOAuth2AuthorizationRequestRepository(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, OAuth2AuthorizationRequest> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.afterPropertiesSet();
        this.redisTemplate = template;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(STATE_PARAM_NAME);
        if (!StringUtils.hasText(state)) {
            return null;
        }
        return redisTemplate.opsForValue().get(KEY_PREFIX + state);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + authorizationRequest.getState(), authorizationRequest, TTL);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            redisTemplate.delete(KEY_PREFIX + authorizationRequest.getState());
        }
        return authorizationRequest;
    }
}
