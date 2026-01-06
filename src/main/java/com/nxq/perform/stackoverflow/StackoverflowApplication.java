package com.nxq.perform.stackoverflow;

import com.nxq.perform.stackoverflow.service.MigrationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StackoverflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(StackoverflowApplication.class, args);
	}

//	@Bean
//	CommandLineRunner run(MigrationService migrationService) {
//		return args -> {
//			System.out.println("--- START MIGRATION ---");
//			migrationService.migratePosts();
//			System.out.println("--- END MIGRATION ---");
//		};
//	}
}
