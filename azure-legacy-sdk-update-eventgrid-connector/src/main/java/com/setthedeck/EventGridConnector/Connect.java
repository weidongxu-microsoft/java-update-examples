package com.setthedeck.EventGridConnector;

import com.microsoft.azure.eventgrid.EventGridClient;
import com.microsoft.azure.eventgrid.TopicCredentials;
import com.microsoft.azure.eventgrid.implementation.EventGridClientImpl;
import com.microsoft.azure.eventgrid.models.EventGridEvent;
import org.joda.time.DateTime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Connect {
    public void Push() throws URISyntaxException {
        // Create an event grid client.
        TopicCredentials topicCredentials = new TopicCredentials(System.getenv("EVENTGRID_TOPIC_KEY"));
        EventGridClient client = new EventGridClientImpl(topicCredentials);

// Publish custom events to the EventGrid.
        System.out.println("Publish custom events to the EventGrid");
        List<EventGridEvent> eventsList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            eventsList.add(new EventGridEvent(
                    UUID.randomUUID().toString(),
                    String.format("Door%d", i),
                    new ContosoItemReceivedEventData("Contoso Item SKU #1"),
                    "Contoso.Items.ItemReceived",
                    DateTime.now(),
                    "2.0"
            ));
        }

        String eventGridEndpoint = String.format("https://%s/", new URI(System.getenv("EVENTGRID_TOPIC_ENDPOINT")).getHost());

        client.publishEvents(eventGridEndpoint, eventsList);
    }
}
