package com.nedbank.kafka.filemanage.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "str-ecp-batch-composition", groupId = "consumer-group")
    public void consumeMessage(String message) {
        // Logic to process the Kafka message
        System.out.println("Consumed Message: " + message);
        // Further processing like file fetching, Azure blob handling, etc.
    }
}
