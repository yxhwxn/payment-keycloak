package com.example.appcard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.appcard.feign.CardIssuerClient;

@RestController
public class HelloController {
    private final CardIssuerClient client;

    public HelloController(CardIssuerClient client) {
        this.client = client;
    }

    @GetMapping("/hello")
    public String hello() {
        String next = client.hello();
        return "hello keycloak! i'm appcard-server\n-> " + next;
    }
}
