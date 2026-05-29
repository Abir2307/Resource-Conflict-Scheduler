package com.resolver.resource_conflict_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResourceConflictSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceConflictSystemApplication.class, args);
	}

}
