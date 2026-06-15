package com.utopios.tickets.event_service.service;

import com.utopios.tickets.event_service.model.Event;
import com.utopios.tickets.event_service.web.CreateEventRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventService {

    private final ConcurrentHashMap<String, Event> events = new ConcurrentHashMap<>();

    public Event create(CreateEventRequest request) {
        String id = UUID.randomUUID().toString();

        Event event = new Event();
        event.setId(id);
        event.setName(request.getName());
        event.setLocation(request.getLocation());
        event.setDate(request.getDate());
        event.setTotalCapacity(request.getTotalCapacity());
        event.setAvailableSeats(request.getTotalCapacity());
        event.setUnitPrice(request.getUnitPrice());

        events.put(id, event);
        return event;
    }

    public List<Event> findAll() {
        return new ArrayList<>(events.values());
    }

    public Event findById(String id) {
        Event event = events.get(id);
        if (event == null) {
            throw new IllegalArgumentException("Event not found: " + id);
        }
        return event;
    }

    public Event reserveSeats(String id, int quantity) {
        Event event = findById(id);

        synchronized (event) {
            if (event.getAvailableSeats() < quantity) {
                throw new IllegalStateException("Not enough seats available");
            }
            event.setAvailableSeats(event.getAvailableSeats() - quantity);
        }

        return event;
    }
}