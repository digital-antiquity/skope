package org.digitalantiquity.skope;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ComponentScan(basePackages = { "org.digitalantiquity.skope" }, excludeFilters = {})
@ImportResource(value = { "classpath:/applicationContext.xml" })
public class SkopeConfiguration {

    public SkopeConfiguration() {
        System.setProperty("java.awt.headless", "true");
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer placeholder = new PropertySourcesPlaceholderConfigurer();
        return placeholder;
    }

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
