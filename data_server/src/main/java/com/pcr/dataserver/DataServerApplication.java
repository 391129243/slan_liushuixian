package com.pcr.dataserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ServerSettings.class)
public class DataServerApplication {
    public static void main(String[] args) { SpringApplication.run(DataServerApplication.class, args); }
}
