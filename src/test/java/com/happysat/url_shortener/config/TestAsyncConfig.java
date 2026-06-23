package com.happysat.url_shortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@Profile("test")
public class TestAsyncConfig {

    @Bean(name = "clickEventExecutor")
    public TaskExecutor clickEventExecutor() {
        return new SyncTaskExecutor();
    }
}
