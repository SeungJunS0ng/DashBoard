package com.festapp.dashboar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DashBoarApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashBoarApplication.class, args);
    }

}
