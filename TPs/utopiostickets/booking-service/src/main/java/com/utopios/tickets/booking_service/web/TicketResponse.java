package com.utopios.tickets.booking_service.web;

import java.time.Instant;

public class TicketResponse {

    private String bookingId;
    private String objectKey;
    private String fileName;
    private String downloadUrl;
    private String content;
    private Instant createdAt;

    public String getBookingId() {
        return bookingId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}