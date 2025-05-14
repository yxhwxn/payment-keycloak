package com.example.card_issuer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.example.card_issuer.feign")
public class CardIssuerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardIssuerApplication.class, args);
	}

}
