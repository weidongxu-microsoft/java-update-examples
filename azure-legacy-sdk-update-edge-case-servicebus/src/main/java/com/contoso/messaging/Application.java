package com.contoso.messaging;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageSender;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Application {

    private static final String CONNECTION_STRING =
        "Endpoint=sb://contoso-orders.servicebus.windows.net/;" +
        "SharedAccessKeyName=RootManageSharedAccessKey;" +
        "SharedAccessKey=dGVzdGtleQ==";

    public static void main(String[] args) throws Exception {
        ErrorClassifier errorClassifier = new ErrorClassifier();

        ConnectionStringBuilder inboundBuilder = new ConnectionStringBuilder(
            CONNECTION_STRING + ";EntityPath=orders-inbound");
        ConnectionStringBuilder outboundBuilder = new ConnectionStringBuilder(
            CONNECTION_STRING + ";EntityPath=orders-outbound");

        IMessageSender forwardSender = ClientFactory
            .createMessageSenderFromConnectionStringBuilder(outboundBuilder);

        MessageTransformer enricher = message -> {
            Message enriched = new Message(message.getBody());
            enriched.setMessageId(message.getMessageId());
            enriched.setLabel(message.getLabel());
            Map<String, Object> props = message.getProperties() != null
                ? new HashMap<>(message.getProperties())
                : new HashMap<String, Object>();
            props.put("processedBy", "order-router");
            props.put("processedAt", System.currentTimeMillis());
            enriched.setProperties(props);
            return enriched;
        };

        QueueSessionManager manager = new QueueSessionManager(errorClassifier);
        manager.createWithTransformer(inboundBuilder, enricher, forwardSender);

        System.out.println("Order processor started. Press ENTER to stop.");
        System.in.read();

        manager.closeAll();
        forwardSender.close();
    }
}
