package com.fitian.burntz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(exclude = {
        HttpClientAutoConfiguration.class,
        RestClientAutoConfiguration.class
})
@EnableJpaAuditing
public class BurntzApplication {

	public static void main(String[] args) {
		SpringApplication.run(BurntzApplication.class, args);
	}

}
