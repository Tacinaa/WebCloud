package com.utopios.tickets.event_service.web;

import jakarta.validation.constraints.Min;

public class ReserveSeatsRequest {

    @Min(1)
    private int quantity;

    public ReserveSeatsRequest() {
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}