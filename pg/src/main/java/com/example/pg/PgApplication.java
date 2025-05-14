package com.example.pg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.example.pg.feign")
public class PgApplication {

	public static void main(String[] args) {
		SpringApplication.run(PgApplication.class, args);
	}

}
