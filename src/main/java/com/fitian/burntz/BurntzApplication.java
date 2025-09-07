package com.fitian.burntz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BurntzApplication {

	public static void main(String[] args) {
		SpringApplication.run(BurntzApplication.class, args);
	}

}
