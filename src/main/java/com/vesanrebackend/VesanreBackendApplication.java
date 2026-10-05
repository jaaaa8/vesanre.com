package com.vesanrebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VesanreBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(VesanreBackendApplication.class, args);
	}

}
