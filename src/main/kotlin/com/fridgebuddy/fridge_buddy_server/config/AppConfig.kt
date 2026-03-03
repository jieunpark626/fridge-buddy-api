package com.fridgebuddy.fridge_buddy_server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestClient
import java.util.concurrent.Executor

@Configuration
@EnableAsync
@EnableScheduling
class AppConfig {

    @Bean("aiTaskExecutor")
    fun aiTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("ai-async-")
        executor.initialize()
        return executor
    }

    @Bean
    fun restClient(): RestClient = RestClient.create()
}