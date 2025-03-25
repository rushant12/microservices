package com.nedbank.kafka.filemanage.service;

import jakarta.xml.soap.*;
import org.springframework.stereotype.Service;
//import javax.xml.soap.*;
import java.net.URL;

@Service
public class FileNetService {

    public String fetchFileFromFileNet(String objectId, String repositoryId) throws Exception {
        // SOAP request to fetch file
        String soapEndpointUrl = "https://filenet-endpoint";
        String soapAction = "http://contracts.it.nednet.co.za/Infrastructure/2008/09/EnterpriseContext/getContentStream";

        // Build SOAP request
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();
        URL endpoint = new URL(soapEndpointUrl);
        SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(objectId, repositoryId), endpoint);

        // Process response
        return processSOAPResponse(soapResponse);
    }

    private SOAPMessage createSOAPRequest(String objectId, String repositoryId) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPBody soapBody = envelope.getBody();

        // Add SOAP elements here
        soapBody.addChildElement("getContentStream", "ns")
                .addChildElement("repositoryId").addTextNode(repositoryId)
                .addChildElement("objectId").addTextNode(objectId);

        return soapMessage;
    }

    private String processSOAPResponse(SOAPMessage soapResponse) throws Exception {
        // Process SOAP response and return the file URL or data
        return "File URL or Content";
    }
}
