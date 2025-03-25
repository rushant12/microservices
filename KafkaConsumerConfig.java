package com.nedbank.kafka.filemanage.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.*;
//import org.springframework.kafka.listener.config.MessageListenerContainerConfig;
//import org.springframework.kafka.listener.MessageListenerContainerFactory;
//import org.springframework.kafka.listener.MessageListenerContainer;
//import org.springframework.kafka.listener.MessageListenerConfig;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentMessageListenerContainer<String, String> listenerContainer() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "your-kafka-broker-address");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-group");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);

        MessageListener<String, String> messageListener = new MessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> record) {
                // Handle message
                System.out.println("Received Message: " + record.value());
            }
        };

        ContainerProperties containerProperties = new ContainerProperties("topic");

        return new ConcurrentMessageListenerContainer<>(consumerFactory,  messageListener);
    }
}
