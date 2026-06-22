package com.utopios.tickets.booking_service.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Booking {

    private String id;
    private String customerName;
    private String eventId;
    private String eventName;
    private String location;
    private String date;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String ticketObjectKey;
    private String ticketDownloadUrl;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getTicketObjectKey() {
        return ticketObjectKey;
    }

    public void setTicketObjectKey(String ticketObjectKey) {
        this.ticketObjectKey = ticketObjectKey;
    }

    public String getTicketDownloadUrl() {
        return ticketDownloadUrl;
    }

    public void setTicketDownloadUrl(String ticketDownloadUrl) {
        this.ticketDownloadUrl = ticketDownloadUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}