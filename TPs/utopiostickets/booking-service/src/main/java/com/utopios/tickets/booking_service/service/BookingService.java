package com.utopios.tickets.booking_service.service;

import com.utopios.tickets.booking_service.model.Booking;
import com.utopios.tickets.booking_service.web.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final DynamoDbClient dynamoDb;
    private final String tableName;
    private final RestClient restClient;
    private final String eventServiceUrl;
    private final String ticketServiceUrl;

    public BookingService(
            DynamoDbClient dynamoDb,
            @Value("${aws.dynamodb.bookings-table}") String tableName,
            RestClient restClient,
            @Value("${services.event-service-url}") String eventServiceUrl,
            @Value("${services.ticket-service-url}") String ticketServiceUrl
    ) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
        this.restClient = restClient;
        this.eventServiceUrl = eventServiceUrl;
        this.ticketServiceUrl = ticketServiceUrl;
    }

    public Booking create(CreateBookingRequest request) {
        EventDto event = fetchEvent(request.getEventId());
        reserveSeats(event.getId(), request.getQuantity());

        String bookingId = UUID.randomUUID().toString();
        BigDecimal totalAmount = event.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        TicketResponse ticket = generateTicket(request, event, bookingId, totalAmount);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setCustomerName(request.getCustomerName());
        booking.setEventId(event.getId());
        booking.setEventName(event.getName());
        booking.setLocation(event.getLocation());
        booking.setDate(event.getDate());
        booking.setQuantity(request.getQuantity());
        booking.setUnitPrice(event.getUnitPrice());
        booking.setTotalAmount(totalAmount);
        booking.setTicketObjectKey(ticket.getObjectKey());
        booking.setTicketDownloadUrl(ticket.getDownloadUrl());
        booking.setCreatedAt(Instant.now());

        saveBooking(booking);
        return booking;
    }

    public Booking findById(String id) {
        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found: " + id);
        }

        return mapToBooking(response.item());
    }

    public List<Booking> findByCustomer(String customerName) {
        QueryResponse response = dynamoDb.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName("customerName-index")
                .keyConditionExpression("customerName = :cn")
                .expressionAttributeValues(Map.of(
                        ":cn", AttributeValue.fromS(customerName)
                ))
                .build());

        return response.items().stream()
                .map(this::mapToBooking)
                .collect(Collectors.toList());
    }

    public List<Booking> findAll() {
        ScanResponse response = dynamoDb.scan(ScanRequest.builder()
                .tableName(tableName)
                .build());

        return response.items().stream()
                .map(this::mapToBooking)
                .collect(Collectors.toList());
    }

    private void saveBooking(Booking booking) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(booking.getId()));
        item.put("customerName", AttributeValue.fromS(booking.getCustomerName()));
        item.put("eventId", AttributeValue.fromS(booking.getEventId()));
        item.put("eventName", AttributeValue.fromS(booking.getEventName()));
        item.put("location", AttributeValue.fromS(booking.getLocation()));
        item.put("date", AttributeValue.fromS(booking.getDate()));
        item.put("quantity", AttributeValue.fromN(String.valueOf(booking.getQuantity())));
        item.put("unitPrice", AttributeValue.fromN(booking.getUnitPrice().toPlainString()));
        item.put("totalAmount", AttributeValue.fromN(booking.getTotalAmount().toPlainString()));
        item.put("ticketObjectKey", AttributeValue.fromS(booking.getTicketObjectKey()));
        item.put("ticketDownloadUrl", AttributeValue.fromS(booking.getTicketDownloadUrl()));
        item.put("createdAt", AttributeValue.fromS(booking.getCreatedAt().toString()));

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());
    }

    private Booking mapToBooking(Map<String, AttributeValue> item) {
        Booking booking = new Booking();
        booking.setId(item.get("id").s());
        booking.setCustomerName(item.get("customerName").s());
        booking.setEventId(item.get("eventId").s());
        booking.setEventName(item.get("eventName").s());
        booking.setLocation(item.get("location").s());
        booking.setDate(item.get("date").s());
        booking.setQuantity(Integer.parseInt(item.get("quantity").n()));
        booking.setUnitPrice(new BigDecimal(item.get("unitPrice").n()));
        booking.setTotalAmount(new BigDecimal(item.get("totalAmount").n()));
        booking.setTicketObjectKey(item.get("ticketObjectKey").s());
        booking.setTicketDownloadUrl(item.get("ticketDownloadUrl").s());
        booking.setCreatedAt(Instant.parse(item.get("createdAt").s()));
        return booking;
    }

    private EventDto fetchEvent(String eventId) {
        try {
            return restClient.get()
                    .uri(eventServiceUrl + "/api/events/{id}", eventId)
                    .retrieve()
                    .body(EventDto.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + eventId);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Event service error");
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Event service unavailable");
        }
    }

    private void reserveSeats(String eventId, int quantity) {
        try {
            restClient.post()
                    .uri(eventServiceUrl + "/api/events/{id}/reserve-seats", eventId)
                    .body(new ReserveSeatsRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough seats available");
            }
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + eventId);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Event service error during seat reservation");
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Event service unavailable");
        }
    }

    private TicketResponse generateTicket(CreateBookingRequest request, EventDto event,
                                          String bookingId, BigDecimal totalAmount) {
        GenerateTicketRequest ticketRequest = new GenerateTicketRequest();
        ticketRequest.setBookingId(bookingId);
        ticketRequest.setCustomerName(request.getCustomerName());
        ticketRequest.setEventId(event.getId());
        ticketRequest.setEventName(event.getName());
        ticketRequest.setLocation(event.getLocation());
        ticketRequest.setDate(event.getDate());
        ticketRequest.setQuantity(request.getQuantity());
        ticketRequest.setTotalAmount(totalAmount);

        try {
            return restClient.post()
                    .uri(ticketServiceUrl + "/api/tickets")
                    .body(ticketRequest)
                    .retrieve()
                    .body(TicketResponse.class);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ticket service error");
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Ticket service unavailable");
        }
    }
}
