package com.example.card_issuer;

import com.example.card_issuer.feign.BankClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    private final BankClient client;

    public HelloController(BankClient client) {
        this.client = client;
    }

    @GetMapping("/hello")
    public String hello() {
        String next = client.hello();
        return "hello keycloak! i'm cardissuer-server\n-> " + next;
    }
}
