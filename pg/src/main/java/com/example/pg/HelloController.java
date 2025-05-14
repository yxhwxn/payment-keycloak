package com.example.pg;

import com.example.pg.feign.AppCardClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    private static final Logger log = LoggerFactory.getLogger(HelloController.class);
    private final AppCardClient client;

    public HelloController(AppCardClient client) {
        this.client = client;
    }

    @GetMapping("/hello")
    public String hello() {
        log.info("[PG] /hello 요청 수신, AppCard 호출 시작");
        String downstream = client.hello();
        log.info("[PG] AppCard 응답 수신: {}", downstream.replace("\n", " | "));
        return "hello keycloak! i'm pg-server\n-> " + downstream;
    }
}
