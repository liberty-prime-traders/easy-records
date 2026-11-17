package app.myliberty.easy_records.configuration.messaging.kafka

import app.myliberty.easy_records.messaging.kafka.common.KafkaConstants
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.config.TopicBuilder

@Configuration
class TopicConfig {

    @Bean
    fun catalogEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaConstants.Topics.CATALOG_EVENTS)
            .partitions(6)
            .replicas(1)
            .build()
    }

    @Bean
    fun transactionEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaConstants.Topics.TRANSACTION_EVENTS)
            .partitions(6)
            .replicas(1)
            .build()
    }

    @Bean
    fun transactionDltTopics(): KafkaAdmin.NewTopics {
        val topics = KafkaConstants.ConsumerGroups.Transaction.all.map { group ->
            TopicBuilder.name(KafkaConstants.Topics.transactionDlt(group))
                .partitions(6)
                .replicas(1)
                .build()
        }.toTypedArray()
        return KafkaAdmin.NewTopics(*topics)
    }
}
