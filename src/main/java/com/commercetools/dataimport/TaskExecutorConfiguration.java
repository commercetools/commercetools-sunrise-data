package com.commercetools.dataimport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskExecutorConfiguration {

    @Value("${maxThreads}")
    private int maxThreads;

    @Bean
    public TaskExecutor taskExecutor() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(maxThreads);
        taskExecutor.initialize();
        return taskExecutor;
    }
}
