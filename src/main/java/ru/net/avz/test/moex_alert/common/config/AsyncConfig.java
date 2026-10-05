package ru.net.avz.test.moex_alert.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "tickersExecutor")
    public AsyncTaskExecutor tickersExecutor() {
        return new VirtualThreadTaskExecutor("tickers-vt-");
    }

    @Bean(name = "pricesExecutor")
    public AsyncTaskExecutor pricesExecutor() {
        return new VirtualThreadTaskExecutor("prices-vt-");
    }

    @Bean(name = "alertsExecutor")
    public AsyncTaskExecutor alertsExecutor() {
        return new VirtualThreadTaskExecutor("alerts-vt-");
    }
}