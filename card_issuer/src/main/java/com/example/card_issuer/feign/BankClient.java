package com.example.card_issuer.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "bank-client", url = "http://localhost:8083")
public interface BankClient {
    @GetMapping("/hello")
    String hello();
}
