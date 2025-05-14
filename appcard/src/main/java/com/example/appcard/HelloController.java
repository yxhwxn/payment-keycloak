package com.example.appcard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.appcard.feign.CardIssuerClient;

@RestController
public class HelloController {
    private static final Logger log = LoggerFactory.getLogger(HelloController.class);
    private final CardIssuerClient client;

    public HelloController(CardIssuerClient client) {
        this.client = client;
    }

    @GetMapping("/hello")
    public String hello() {
        log.info("[AppCard] /hello 요청 수신, Card 호출 시작");
        String next = client.hello();
        log.info("[AppCard] Card 응답 수신: {}", next);
        return "hello keycloak! i'm appcard-server\n-> " + next;
    }
}
