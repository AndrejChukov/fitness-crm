package ru.fitnesscrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FitnessCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitnessCrmApplication.class, args);
    }

}
