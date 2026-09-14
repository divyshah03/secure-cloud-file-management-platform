package com.cloudfilemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class CloudFileManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudFileManagerApplication.class, args);
    }
}
