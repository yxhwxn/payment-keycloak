package com.example.pg;

import com.example.pg.feign.AppCardClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    private final AppCardClient client;

    public HelloController(AppCardClient client) {
        this.client = client;
    }

    @GetMapping("/hello")
    public String hello() {
        String downstream = client.hello();
        return "hello keycloak! i'm pg-server\n-> " + downstream;
    }
}
