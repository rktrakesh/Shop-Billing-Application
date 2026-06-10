package com.shopbilling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ShopBillingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopBillingApplication.class, args);
    }
}
