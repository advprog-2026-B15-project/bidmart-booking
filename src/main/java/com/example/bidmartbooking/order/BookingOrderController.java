package com.example.bidmartbooking.order;

import com.example.bidmartbooking.order.dto.CreateOrderRequest;
import com.example.bidmartbooking.order.dto.OrderResponse;
import com.example.bidmartbooking.order.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class BookingOrderController {
    private final BookingOrderService bookingOrderService;

    public BookingOrderController(BookingOrderService bookingOrderService) {
        this.bookingOrderService = bookingOrderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse order = bookingOrderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public List<OrderResponse> findAllOrders() {
        return bookingOrderService.findAllOrders();
    }

    @GetMapping("/{orderId}")
    public OrderResponse findOrderById(@PathVariable Long orderId) {
        return bookingOrderService.findOrderById(orderId);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateOrderStatus(
        @PathVariable Long orderId,
        @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return bookingOrderService.updateOrderStatus(orderId, request);
    }
}
