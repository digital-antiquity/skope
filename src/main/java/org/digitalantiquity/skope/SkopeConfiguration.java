package org.digitalantiquity.skope;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.annotation.ImportResource;
@Configuration
@ComponentScan(basePackages = { "org.digitalantiquity.skope" }, excludeFilters= {})
@ImportResource(value = { "classpath:/applicationContext.xml" })
public class SkopeConfiguration {

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(20);
        pool.setMaxPoolSize(50);
        pool.setThreadNamePrefix("pool-");
        pool.setWaitForTasksToCompleteOnShutdown(true);
        return pool;
    }
}
