package com.utopios.tickets.ticket_service.web;

import java.time.Instant;

public class TicketResponse {

    private String bookingId;
    private String objectKey;
    private String fileName;
    private String downloadUrl;
    private String content;
    private Instant createdAt;

    public TicketResponse(String bookingId, String objectKey, String fileName,
                          String downloadUrl, String content, Instant createdAt) {
        this.bookingId = bookingId;
        this.objectKey = objectKey;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.content = content;
        this.createdAt = createdAt;
    }

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