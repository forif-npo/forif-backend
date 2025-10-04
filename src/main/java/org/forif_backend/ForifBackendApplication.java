package org.forif_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ForifBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForifBackendApplication.class, args);
    }

}
