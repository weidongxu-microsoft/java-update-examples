package com.contoso.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.core.util.BinaryData;

import java.util.HashMap;
import java.util.Map;

public class Application {

    private static final String CONNECTION_STRING =
        "Endpoint=sb://contoso-orders.servicebus.windows.net/;" +
        "SharedAccessKeyName=RootManageSharedAccessKey;" +
        "SharedAccessKey=dGVzdGtleQ==";

    public static void main(String[] args) throws Exception {
        ErrorClassifier errorClassifier = new ErrorClassifier();

        String inboundConnectionString = CONNECTION_STRING + ";EntityPath=orders-inbound";
        String outboundConnectionString = CONNECTION_STRING + ";EntityPath=orders-outbound";

        ServiceBusSenderClient forwardSender = new ServiceBusClientBuilder()
            .connectionString(outboundConnectionString)
            .sender()
            .buildClient();

        MessageTransformer enricher = message -> {
            ServiceBusMessage enriched = new ServiceBusMessage(message.getBody());
            enriched.setMessageId(message.getMessageId());
            enriched.setSubject(message.getSubject());
            
            Map<String, Object> props = message.getApplicationProperties() != null
                ? new HashMap<>(message.getApplicationProperties())
                : new HashMap<String, Object>();
            props.put("processedBy", "order-router");
            props.put("processedAt", System.currentTimeMillis());
            enriched.getApplicationProperties().putAll(props);
            return enriched;
        };

        QueueSessionManager manager = new QueueSessionManager(errorClassifier);
        manager.createWithTransformer(inboundConnectionString, "orders-inbound", enricher, forwardSender);

        System.out.println("Order processor started. Press ENTER to stop.");
        System.in.read();

        manager.closeAll();
        forwardSender.close();
    }
}
