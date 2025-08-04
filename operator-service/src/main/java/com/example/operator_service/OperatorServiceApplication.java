package com.example.operator_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients

public class OperatorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OperatorServiceApplication.class, args);
	}

}
