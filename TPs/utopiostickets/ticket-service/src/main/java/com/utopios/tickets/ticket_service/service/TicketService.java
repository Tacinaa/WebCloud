package com.utopios.tickets.ticket_service.service;

import com.utopios.tickets.ticket_service.web.GenerateTicketRequest;
import com.utopios.tickets.ticket_service.web.TicketResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;

@Service
public class TicketService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final long downloadTtlSeconds;

    public TicketService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.tickets-bucket}") String bucketName,
            @Value("${ticket.download-ttl-seconds:900}") long downloadTtlSeconds
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.downloadTtlSeconds = downloadTtlSeconds;
    }

    public TicketResponse generate(GenerateTicketRequest request) {
        String objectKey = "tickets/" + request.getBookingId() + ".txt";
        String content = buildContent(request);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType("text/plain; charset=utf-8")
                        .build(),
                RequestBody.fromString(content)
        );

        String downloadUrl = buildPresignedUrl(objectKey);

        return new TicketResponse(
                request.getBookingId(),
                objectKey,
                request.getBookingId() + ".txt",
                downloadUrl,
                content,
                Instant.now()
        );
    }

    public TicketResponse regenerateUrl(String bookingId) {
        String objectKey = "tickets/" + bookingId + ".txt";

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable : " + bookingId);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur S3 : " + e.awsErrorDetails().errorMessage());
        }

        return new TicketResponse(bookingId, objectKey, bookingId + ".txt",
                buildPresignedUrl(objectKey), null, null);
    }

    public String download(String bookingId) {
        String objectKey = "tickets/" + bookingId + ".txt";
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build()).asUtf8String();
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable : " + bookingId);
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur S3 : " + e.awsErrorDetails().errorMessage());
        }
    }

    private String buildPresignedUrl(String objectKey) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(downloadTtlSeconds))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
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
