package com.controlpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ControlProApplication {
    public static void main(String[] args) {
        SpringApplication.run(ControlProApplication.class, args);
    }
}
