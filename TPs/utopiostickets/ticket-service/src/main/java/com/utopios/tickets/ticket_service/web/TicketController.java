package com.utopios.tickets.ticket_service.web;

import com.utopios.tickets.ticket_service.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> generate(@Valid @RequestBody GenerateTicketRequest request) {
        return ResponseEntity.status(201).body(ticketService.generate(request));
    }

    @GetMapping("/{bookingId}/download-url")
    public TicketResponse regenerateUrl(@PathVariable String bookingId) {
        return ticketService.regenerateUrl(bookingId);
    }

    @GetMapping("/{bookingId}/download")
    public ResponseEntity<String> download(@PathVariable String bookingId) {
        String content = ticketService.download(bookingId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + bookingId + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping("/ping")
    public String ping() {
        return "ticket-service ok";
    }
}