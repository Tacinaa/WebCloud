package com.utopios.tickets.event_service.model;

import java.math.BigDecimal;

public class Event {

    private String id;
    private String name;
    private String location;
    private String date;
    private int totalCapacity;
    private int availableSeats;
    private BigDecimal unitPrice;

    public Event() {
    }

    public Event(String id, String name, String location, String date, int totalCapacity, int availableSeats, BigDecimal unitPrice) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.date = date;
        this.totalCapacity = totalCapacity;
        this.availableSeats = availableSeats;
        this.unitPrice = unitPrice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}