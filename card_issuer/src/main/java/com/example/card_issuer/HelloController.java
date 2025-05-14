package com.example.card_issuer;

import com.example.card_issuer.feign.BankClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    private static final Logger log = LoggerFactory.getLogger(HelloController.class);
    private final BankClient client;

    public HelloController(BankClient client) {
        this.client = client;
    }

    @GetMapping("/hello")
    public String hello() {
        log.info("[Card] /hello 요청 수신, Bank 호출 시작");
        String next = client.hello();
        log.info("[Card] Bank 응답 수신: {}", next);
        return "hello keycloak! i'm card-server\n-> " + next;
    }
}
