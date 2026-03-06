package com.github.aliandr13.zenmo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ZenmoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZenmoApplication.class, args);
    }

}
