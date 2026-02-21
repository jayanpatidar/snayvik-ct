package com.snayvik.kpi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
public class KpiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KpiApplication.class, args);
    }
}
