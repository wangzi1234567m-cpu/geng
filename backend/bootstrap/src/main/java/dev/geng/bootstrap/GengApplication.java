package dev.geng.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "dev.geng")
public class GengApplication {
    public static void main(String[] args) {
        SpringApplication.run(GengApplication.class, args);
    }
}
