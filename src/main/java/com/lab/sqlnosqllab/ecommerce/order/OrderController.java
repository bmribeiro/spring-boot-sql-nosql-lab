package com.lab.sqlnosqllab.ecommerce.order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody OrderService.CreateOrderRequest request) {

        Order created = orderService.createOrder(idempotencyKey, request);
        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + created.getId()))
                .body(created);
    }

    @GetMapping("/users/{userId}/orders")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable UUID userId) {
        return ResponseEntity.ok(orderRepository.findByUserIdWithItems(userId));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}