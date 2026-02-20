package com.example.bidmartbooking.order;

import com.example.bidmartbooking.order.dto.CreateOrderRequest;
import com.example.bidmartbooking.order.dto.OrderResponse;
import com.example.bidmartbooking.order.dto.UpdateOrderStatusRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingOrderService {
    private final BookingOrderRepository bookingOrderRepository;

    public BookingOrderService(BookingOrderRepository bookingOrderRepository) {
        this.bookingOrderRepository = bookingOrderRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        BookingOrder order = new BookingOrder();
        order.setItemName(request.getItemName().trim());
        order.setQuantity(request.getQuantity());
        order.setNotes(request.getNotes());
        order.setStatus(OrderStatus.PENDING);

        BookingOrder savedOrder = bookingOrderRepository.save(order);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAllOrders() {
        return bookingOrderRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findOrderById(Long orderId) {
        return toResponse(getOrderEntityById(orderId));
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        BookingOrder order = getOrderEntityById(orderId);
        order.setStatus(request.getStatus());
        return toResponse(order);
    }

    private BookingOrder getOrderEntityById(Long orderId) {
        return bookingOrderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order with id " + orderId + " was not found"
            ));
    }

    private OrderResponse toResponse(BookingOrder order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setItemName(order.getItemName());
        response.setQuantity(order.getQuantity());
        response.setNotes(order.getNotes());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }
}
