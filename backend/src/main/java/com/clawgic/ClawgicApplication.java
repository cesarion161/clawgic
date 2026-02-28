package com.clawgic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClawgicApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClawgicApplication.class, args);
    }
}
