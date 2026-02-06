package com.wakilfly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WakilflyApplication {

    public static void main(String[] args) {
        SpringApplication.run(WakilflyApplication.class, args);
    }
}
