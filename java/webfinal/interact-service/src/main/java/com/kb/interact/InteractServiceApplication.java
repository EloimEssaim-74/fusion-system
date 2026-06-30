package com.kb.interact;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.kb")
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.kb.interact.mapper")
public class InteractServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InteractServiceApplication.class, args);
    }
}
