package com.example.pg.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "appcard-client", url = "http://localhost:8081")
public interface AppCardClient {

    @GetMapping("/hello")
    String hello();
}
