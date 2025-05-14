package com.example.pg.config;

import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
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
            OAuth2AuthorizedClientRepository clientRepository   // 기존 주입 그대로
    ) {
        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var manager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrations, clientRepository            // Service 대신 Repository 주입
        );
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    // 2) Feign 인터셉터: downstream 호출 시 자동으로 토큰 헤더 추가
    @Bean
    public RequestInterceptor oauth2FeignInterceptor(OAuth2AuthorizedClientManager manager) {
        return request -> {
            var authRequest = OAuth2AuthorizeRequest.withClientRegistrationId("pg-server")
                    .principal("pg-server")
                    .build();
            var client = manager.authorize(authRequest);
            request.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        };
    }

    // 3) /hello 엔드포인트 JWT 인증 적용
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/hello").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );
        return http.build();
    }
}











