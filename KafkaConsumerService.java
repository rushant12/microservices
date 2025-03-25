package com.nedbank.kafka.filemanage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private final FileNetService fileNetService;
    private final ObjectMapper objectMapper; // Jackson ObjectMapper for parsing JSON

    // Constructor Injection of FileNetService and ObjectMapper
    public KafkaConsumerService(FileNetService fileNetService, ObjectMapper objectMapper) {
        this.fileNetService = fileNetService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "str-ecp-batch-composition", groupId = "str-ecp-batch*")
    public void consumeMessage(String message) {
        System.out.println("Consumed Message: " + message);

        try {
            // Parse the JSON message
            JsonNode jsonNode = objectMapper.readTree(message);
            
            // Extract Header data
            JsonNode header = jsonNode.get("Header");
            String tenantCode = header.get("TenantCode").asText();
            String channelId = header.get("ChannelID").asText();
            String sourceSystem = header.get("SourceSystem").asText();
            String jobName = header.get("JobName").asText();
            
            // Print extracted Header data for logging
            System.out.println("Header -> TenantCode: " + tenantCode);
            System.out.println("Header -> ChannelID: " + channelId);
            System.out.println("Header -> SourceSystem: " + sourceSystem);
            System.out.println("Header -> JobName: " + jobName);

            // Extract Payload data
            JsonNode payload = jsonNode.get("Payload");
            String objectId = payload.get("ObjectId").asText();
            String repositoryId = payload.get("RepositoryId").asText();

            // Print extracted Payload data
            System.out.println("Payload -> ObjectId: " + objectId);
            System.out.println("Payload -> RepositoryId: " + repositoryId);

            // Fetch the file from FileNet
            String fileContent = fileNetService.fetchFileFromFileNet(objectId, repositoryId);
            System.out.println("Fetched File Content: " + fileContent);

            // Further processing like uploading to Azure Blob can be done here

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error fetching file from FileNet: " + e.getMessage());
        }
    }
}
