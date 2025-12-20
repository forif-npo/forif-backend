package org.forif_backend.infrastructure.external.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.concurrent.Executor;

@Configuration
public class NotificationConfig {

    @Bean(destroyMethod = "close")
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 핵심 스레드 수: 동시에 실행할 기본 스레드 개수
        executor.setCorePoolSize(5);
        // 최대 스레드 수: 요청이 많아질 때 확장될 최대 개수
        executor.setMaxPoolSize(10);
        // 큐 용량: 스레드가 꽉 찼을 때 대기하는 작업 수
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AlimTalk-");
        executor.initialize();
        return executor;
    }
}