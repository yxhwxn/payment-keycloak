package com.example.appcard.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "card-client", url = "http://localhost:8082")
public interface CardIssuerClient {
    @GetMapping("/hello")
    String hello();
}
