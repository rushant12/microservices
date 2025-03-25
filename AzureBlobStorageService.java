package com.nedbank.kafka.filemanage.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class AzureBlobStorageService {

    private final BlobServiceClient blobServiceClient;

    public AzureBlobStorageService() {
        // Initialize BlobServiceClient with connection string
        try {
            this.blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString("DefaultEndpointsProtocol=http;AccountName=myazurestorageaccount;AccountKey=your_account_key_here;EndpointSuffix=core.windows.net")
                    .buildClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String uploadFile(String containerName, String blobName, String fileContent) {
        // Code to upload a file to Azure Blob Storage
        com.azure.storage.blob.BlobClient blobClient;
        try {
            var containerClient = blobServiceClient.getBlobContainerClient(containerName);
            blobClient = containerClient.getBlobClient(blobName);
            // Convert binary data to InputStream
            byte[] binaryData = fileContent.getBytes();
            InputStream inputStream = new ByteArrayInputStream(binaryData);
            blobClient.upload(inputStream, binaryData.length);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return blobClient.getBlobUrl();
    }
}
