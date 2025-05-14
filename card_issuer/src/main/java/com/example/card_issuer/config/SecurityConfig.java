package com.example.card_issuer.config;

import feign.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;

@Configuration
public class SecurityConfig {

    @Bean
    @Primary
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clients,
            OAuth2AuthorizedClientRepository clientRepo
    ) {
        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        var mgr = new DefaultOAuth2AuthorizedClientManager(clients, clientRepo);
        mgr.setAuthorizedClientProvider(provider);
        return mgr;
    }

    @Bean
    public RequestInterceptor oauth2FeignInterceptor(OAuth2AuthorizedClientManager manager) {
        Logger log = LoggerFactory.getLogger("card-Feign");
        return request -> {
            log.info("[Card] Feign 호출 전 – URL: {}", request.url());

            var authReq = OAuth2AuthorizeRequest.withClientRegistrationId("card-server")
                    .principal("card-server")
                    .build();
            var client = manager.authorize(authReq);

            log.info("[Card] 받은 Access Token (truncated): {}…",
                    client.getAccessToken().getTokenValue().substring(0, 8));

            request.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
                        .requestMatchers("/hello").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );
        return http.build();
    }
}
