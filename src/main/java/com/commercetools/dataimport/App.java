package com.commercetools.dataimport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableBatchProcessing
@EnableCaching
@Slf4j
public class App {

    public static void main(String[] args) {
        try (final ConfigurableApplicationContext ctx = SpringApplication.run(App.class, args)) {
            ctx.close();
        }
    }
}
