package com.utopios.tickets.report_job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final DynamoDbClient dynamoDb;
    private final S3Client s3Client;
    private final String bookingsTable;
    private final String reportsBucket;
    private final ObjectMapper objectMapper;

    public ReportService(
            DynamoDbClient dynamoDb,
            S3Client s3Client,
            @Value("${aws.dynamodb.bookings-table}") String bookingsTable,
            @Value("${aws.s3.reports-bucket}") String reportsBucket
    ) {
        this.dynamoDb = dynamoDb;
        this.s3Client = s3Client;
        this.bookingsTable = bookingsTable;
        this.reportsBucket = reportsBucket;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void generateAndExport() {
        System.out.println("[report-job] Scanning bookings table: " + bookingsTable);
        List<Map<String, Object>> bookings = scanAllBookings();

        LocalDate today = LocalDate.now();
        Map<String, Object> report = new HashMap<>();
        report.put("exportDate", today.toString());
        report.put("totalBookings", bookings.size());
        report.put("bookings", bookings);

        String objectKey = String.format("reports/%d/%02d/%02d/report.json",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        try {
            String json = objectMapper.writeValueAsString(report);

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(reportsBucket)
                            .key(objectKey)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromString(json)
            );

            System.out.printf("[report-job] Exported %d bookings to s3://%s/%s%n",
                    bookings.size(), reportsBucket, objectKey);
        } catch (Exception e) {
            System.err.println("[report-job] Failed to export report: " + e.getMessage());
            throw new RuntimeException("Report export failed", e);
        }
    }

    private List<Map<String, Object>> scanAllBookings() {
        ScanResponse response = dynamoDb.scan(ScanRequest.builder()
                .tableName(bookingsTable)
                .build());

        return response.items().stream()
                .map(this::flattenItem)
                .collect(Collectors.toList());
    }

    private Map<String, Object> flattenItem(Map<String, AttributeValue> item) {
        Map<String, Object> result = new HashMap<>();
        item.forEach((key, value) -> {
            if (value.s() != null) result.put(key, value.s());
            else if (value.n() != null) result.put(key, value.n());
            else if (value.bool() != null) result.put(key, value.bool());
        });
        return result;
    }
}
