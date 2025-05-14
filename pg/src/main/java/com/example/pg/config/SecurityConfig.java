package com.example.pg.config;

import feign.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableFeignClients
public class SecurityConfig {

    // 1) Client Credentials용 AuthorizedClientManager
    @Bean
    @Primary
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrations,
            OAuth2AuthorizedClientRepository clientRepository
    ) {
        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        var manager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrations, clientRepository
        );
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    // 2) Feign 인터셉터: AppCard 호출 시 토큰 헤더 자동 추가
    @Bean
    public RequestInterceptor oauth2FeignInterceptor(OAuth2AuthorizedClientManager manager) {
        Logger log = LoggerFactory.getLogger("pg-server-Feign");
        return request -> {
            log.info("[PG] Feign 호출 전 – URL: {}", request.url());

            var authReq = OAuth2AuthorizeRequest
                    .withClientRegistrationId("pg-server")
                    .principal("pg-server")
                    .build();
            var client = manager.authorize(authReq);

            log.info("[PG] 받은 Access Token (truncated): {}…",
                    client.getAccessToken().getTokenValue().substring(0, 8));

            request.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        };
    }

    // 3) 외부 요청은 모두 허용 (Resource Server 설정 제거)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}











