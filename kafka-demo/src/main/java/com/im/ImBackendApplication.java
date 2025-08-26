package com.im;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class ImBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImBackendApplication.class, args);
    }
}
