package com.nedbank.kafka.filemanage.service;

import jakarta.xml.soap.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URL;

@Service
public class FileNetService {

    @Value("${filenet.soap.endpoint}")
    private String soapEndpointUrl;

    @Value("${filenet.soap.action}")
    private String soapAction;

    public String fetchFileFromFileNet(String objectId, String repositoryId) throws Exception {
        try {
            SOAPMessage request = createSOAPRequest(objectId, repositoryId);

            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();
            soapConnection.setTimeout(30000);

            URL endpoint = new URL(soapEndpointUrl);
            SOAPMessage soapResponse = soapConnection.call(request, endpoint);

            return processSOAPResponse(soapResponse);
        } catch (Exception e) {
            System.err.println("Error in FileNetService: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Error fetching file from FileNet", e);
        }
    }

    private SOAPMessage createSOAPRequest(String objectId, String repositoryId) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPBody soapBody = envelope.getBody();

        soapBody.addChildElement("getContentStream", "ns")
                .addChildElement("repositoryId").addTextNode(repositoryId)
                .addChildElement("objectId").addTextNode(objectId);

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", soapAction);

        return soapMessage;
    }

    private String processSOAPResponse(SOAPMessage soapResponse) throws Exception {
        SOAPBody responseBody = soapResponse.getSOAPBody();

        if (responseBody != null && responseBody.getElementsByTagName("fileContent") != null) {
            String fileContent = responseBody.getElementsByTagName("fileContent").item(0).getTextContent();
            return fileContent;
        } else {
            return "Error: No file content found in the response";
        }
    }
}
