package com.deundeun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProovyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProovyApplication.class, args);
    }

}
