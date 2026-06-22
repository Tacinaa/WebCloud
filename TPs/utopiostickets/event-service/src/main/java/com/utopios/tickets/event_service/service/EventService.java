package com.utopios.tickets.event_service.service;

import com.utopios.tickets.event_service.model.Event;
import com.utopios.tickets.event_service.web.CreateEventRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final DynamoDbClient dynamoDb;
    private final String tableName;

    public EventService(DynamoDbClient dynamoDb,
                        @Value("${aws.dynamodb.events-table}") String tableName) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
    }

    public Event create(CreateEventRequest request) {
        String id = UUID.randomUUID().toString();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(id));
        item.put("name", AttributeValue.fromS(request.getName()));
        item.put("location", AttributeValue.fromS(request.getLocation()));
        item.put("date", AttributeValue.fromS(request.getDate()));
        item.put("totalCapacity", AttributeValue.fromN(String.valueOf(request.getTotalCapacity())));
        item.put("availableSeats", AttributeValue.fromN(String.valueOf(request.getTotalCapacity())));
        item.put("unitPrice", AttributeValue.fromN(request.getUnitPrice().toPlainString()));

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());

        return mapToEvent(item);
    }

    public List<Event> findAll() {
        ScanResponse response = dynamoDb.scan(ScanRequest.builder()
                .tableName(tableName)
                .build());
        return response.items().stream()
                .map(this::mapToEvent)
                .collect(Collectors.toList());
    }

    public Event findById(String id) {
        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            throw new IllegalArgumentException("Event not found: " + id);
        }

        return mapToEvent(response.item());
    }

    public Event reserveSeats(String id, int quantity) {
        // Throws IllegalArgumentException if event not found
        findById(id);

        try {
            UpdateItemResponse response = dynamoDb.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("id", AttributeValue.fromS(id)))
                    .updateExpression("SET availableSeats = availableSeats - :qty")
                    .conditionExpression("availableSeats >= :qty")
                    .expressionAttributeValues(Map.of(
                            ":qty", AttributeValue.fromN(String.valueOf(quantity))
                    ))
                    .returnValues(ReturnValue.ALL_NEW)
                    .build());

            return mapToEvent(response.attributes());
        } catch (ConditionalCheckFailedException e) {
            throw new IllegalStateException("Not enough seats available");
        }
    }

    private Event mapToEvent(Map<String, AttributeValue> item) {
        Event event = new Event();
        event.setId(item.get("id").s());
        event.setName(item.get("name").s());
        event.setLocation(item.get("location").s());
        event.setDate(item.get("date").s());
        event.setTotalCapacity(Integer.parseInt(item.get("totalCapacity").n()));
        event.setAvailableSeats(Integer.parseInt(item.get("availableSeats").n()));
        event.setUnitPrice(new BigDecimal(item.get("unitPrice").n()));
        return event;
    }
}
