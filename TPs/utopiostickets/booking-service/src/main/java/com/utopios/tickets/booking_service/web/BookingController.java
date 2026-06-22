package com.utopios.tickets.booking_service.web;

import com.utopios.tickets.booking_service.model.Booking;
import com.utopios.tickets.booking_service.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(request);
    }

    @GetMapping("/{id}")
    public Booking findById(@PathVariable String id) {
        return bookingService.findById(id);
    }

    @GetMapping
    public List<Booking> findAll(@RequestParam(required = false) String customerName) {
        if (customerName != null && !customerName.isBlank()) {
            return bookingService.findByCustomer(customerName);
        }
        return bookingService.findAll();
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "booking-service", "status", "ok");
    }
}