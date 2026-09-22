package com.fund.valuation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ValuationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValuationApplication.class, args);
    }
}
