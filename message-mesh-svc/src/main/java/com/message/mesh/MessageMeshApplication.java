package com.message.mesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class MessageMeshApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageMeshApplication.class, args);
    }
}
