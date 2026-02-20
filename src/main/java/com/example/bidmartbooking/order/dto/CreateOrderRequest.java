package com.example.bidmartbooking.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateOrderRequest {
    @NotBlank(message = "itemName is required")
    @Size(max = 150, message = "itemName max length is 150")
    private String itemName;

    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @Size(max = 500, message = "notes max length is 500")
    private String notes;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
