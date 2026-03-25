package com.sqljudge.containermanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ContainerManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContainerManagerApplication.class, args);
    }
}