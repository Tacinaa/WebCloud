package com.utopios.tickets.ticket_service.service;

import com.utopios.tickets.ticket_service.model.TicketMetadata;
import com.utopios.tickets.ticket_service.web.GenerateTicketRequest;
import com.utopios.tickets.ticket_service.web.TicketResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TicketService {

    private final Map<String, TicketMetadata> tickets = new ConcurrentHashMap<>();
    private final Path storageDir;
    private final String publicBaseUrl;
    private final long downloadTtlSeconds;

    public TicketService(
            @Value("${ticket.storage-dir:./data/tickets}") String storageDir,
            @Value("${ticket.public-base-url:http://localhost:8082}") String publicBaseUrl,
            @Value("${ticket.download-ttl-seconds:900}") long downloadTtlSeconds
    ) {
        this.storageDir = Paths.get(storageDir);
        this.publicBaseUrl = publicBaseUrl;
        this.downloadTtlSeconds = downloadTtlSeconds;
    }

    public TicketResponse generate(GenerateTicketRequest request) {
        try {
            Files.createDirectories(storageDir);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de créer le dossier des billets");
        }

        String objectKey = "tickets/" + request.getBookingId() + ".txt";
        String fileName = request.getBookingId() + ".txt";
        String content = buildContent(request);
        Path path = storageDir.resolve(fileName);

        try {
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'écrire le billet");
        }

        String downloadUrl = publicBaseUrl + "/api/tickets/" + request.getBookingId() + "/download?expiresIn=" + downloadTtlSeconds;

        TicketMetadata metadata = new TicketMetadata();
        metadata.setBookingId(request.getBookingId());
        metadata.setObjectKey(objectKey);
        metadata.setFileName(fileName);
        metadata.setDownloadUrl(downloadUrl);
        metadata.setCreatedAt(Instant.now());

        tickets.put(request.getBookingId(), metadata);

        return new TicketResponse(
                metadata.getBookingId(),
                metadata.getObjectKey(),
                metadata.getFileName(),
                metadata.getDownloadUrl(),
                content,
                metadata.getCreatedAt()
        );
    }

    public TicketResponse regenerateUrl(String bookingId) {
        TicketMetadata metadata = tickets.get(bookingId);
        if (metadata == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable : " + bookingId);
        }

        String downloadUrl = publicBaseUrl + "/api/tickets/" + bookingId + "/download?expiresIn=" + downloadTtlSeconds;
        metadata.setDownloadUrl(downloadUrl);

        return new TicketResponse(
                metadata.getBookingId(),
                metadata.getObjectKey(),
                metadata.getFileName(),
                metadata.getDownloadUrl(),
                null,
                metadata.getCreatedAt()
        );
    }

    public String download(String bookingId) {
        Path path = storageDir.resolve(bookingId + ".txt");

        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable : " + bookingId);
        }

        try {
            return Files.readString(path);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de lire le billet");
        }
    }

    private String buildContent(GenerateTicketRequest request) {
        return String.join(System.lineSeparator(),
                "=== UTOPIOS TICKET ===",
                "Booking ID: " + request.getBookingId(),
                "Customer: " + request.getCustomerName(),
                "Event ID: " + request.getEventId(),
                "Event: " + request.getEventName(),
                "Location: " + request.getLocation(),
                "Date: " + request.getDate(),
                "Quantity: " + request.getQuantity(),
                "Total amount: " + request.getTotalAmount()
        );
    }
}