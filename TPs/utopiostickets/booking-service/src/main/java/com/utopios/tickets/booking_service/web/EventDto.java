package com.utopios.tickets.booking_service.web;

import java.math.BigDecimal;

public class EventDto {

    private String id;
    private String name;
    private String location;
    private String date;
    private int totalCapacity;
    private int availableSeats;
    private BigDecimal unitPrice;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getDate() {
        return date;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}