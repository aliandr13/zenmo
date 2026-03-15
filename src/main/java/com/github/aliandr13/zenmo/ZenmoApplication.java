package com.github.aliandr13.zenmo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Spring Boot application entry point.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ZenmoApplication {

    /**
     * Application entry point.
     */
    static void main(String[] args) {
        SpringApplication.run(ZenmoApplication.class, args);
    }

}
