package com.utopios.tickets.booking_service.web;

import jakarta.validation.constraints.Min;

public class ReserveSeatsRequest {

    @Min(1)
    private int quantity;

    public ReserveSeatsRequest() {
    }

    public ReserveSeatsRequest(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}