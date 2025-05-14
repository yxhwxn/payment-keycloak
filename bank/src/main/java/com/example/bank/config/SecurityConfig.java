package com.example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import feign.RequestInterceptor;

@Configuration
public class SecurityConfig {

    // Client Credentials Grant를 처리할 AuthorizedClientManager
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clients,
            OAuth2AuthorizedClientRepository authClients) {

        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, authClients);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    // Feign 호출 시 토큰을 자동으로 헤더에 붙여주는 인터셉터
    @Bean
    public RequestInterceptor oauth2FeignInterceptor(OAuth2AuthorizedClientManager manager) {
        return request -> {
            // 이 registrationId 를 각 서비스에 맞게 바꿔주시면 됩니다
            String registrationId =
                    // 예: pg-server 모듈에서는 "pg-server"
                    //     appcard 모듈에서는 "appcard-server" 등
                    request.url().contains("8081") ? "appcard-server" :
                            request.url().contains("8082") ? "cardissuer-server" :
                                    request.url().contains("8083") ? "bank-server" :
                                            "pg-server";

            var authRequest = OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
                    .principal(registrationId)
                    .build();
            var client = manager.authorize(authRequest);
            request.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        };
    }

    // /hello 는 JWT 검증 후 허가
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt()
                );
        return http.build();
    }
}
