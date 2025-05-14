package com.example.appcard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.example.appcard.feign")
public class AppcardApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppcardApplication.class, args);
	}

}
