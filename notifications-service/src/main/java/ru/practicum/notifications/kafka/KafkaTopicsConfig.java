package ru.practicum.notifications.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    // общий топик для всех уведомлений (ключ = login)
    public static final String NOTIFICATIONS_TOPIC = "bank.notifications";
    public static final String NOTIFICATIONS_DLT_TOPIC = "bank.notifications.dlt";

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(NOTIFICATIONS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsDltTopic() {
        return TopicBuilder.name(NOTIFICATIONS_DLT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
