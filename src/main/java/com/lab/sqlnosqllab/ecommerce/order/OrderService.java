package com.lab.sqlnosqllab.ecommerce.order;

import com.lab.sqlnosqllab.ecommerce.enums.OrderStatus;
import com.lab.sqlnosqllab.ecommerce.product.Product;
import com.lab.sqlnosqllab.ecommerce.product.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final TransactionTemplate transactionTemplate;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                        ProductService productService, TransactionTemplate transactionTemplate) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productService = productService;
        this.transactionTemplate = transactionTemplate;
    }

    public record OrderItemRequest(String productId, int quantity) {
    }

    public record CreateOrderRequest(UUID userId, List<OrderItemRequest> items) {
    }

    public Order createOrder(String idempotencyKey, CreateOrderRequest request) {
        // 1. Idempotency check
        Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Validate that the user exists in PostgreSQL
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        // 3. Reserve stock in MongoDB (keeping a record in case rollback is needed)
        Map<String, Integer> reservedItems = new LinkedHashMap<>();
        List<Product> reservedProducts = new ArrayList<>();

        try {
            for (OrderItemRequest itemReq : request.items()) {
                Product product = productService.reserveStock(itemReq.productId(), itemReq.quantity());
                reservedItems.put(itemReq.productId(), itemReq.quantity());
                reservedProducts.add(product);
            }

            // 4. Persist the order and price snapshots in an ACID transaction in PostgreSQL
            return transactionTemplate.execute(status -> {
                Order order = new Order();
                order.setUser(user);
                order.setIdempotencyKey(idempotencyKey);

                BigDecimal total = BigDecimal.ZERO;
                for (int i = 0; i < request.items().size(); i++) {
                    OrderItemRequest reqItem = request.items().get(i);
                    Product prod = reservedProducts.get(i);

                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setProductId(prod.getId());
                    orderItem.setUnitPrice(prod.getPrice()); // Data snapshotting
                    orderItem.setQuantity(reqItem.quantity());

                    order.getItems().add(orderItem);
                    total = total.add(prod.getPrice().multiply(BigDecimal.valueOf(reqItem.quantity())));
                }

                order.setTotalAmount(total);
                return orderRepository.save(order);
            });

        } catch (Exception ex) {
            // 5. COMPENSATING ACTION (Saga Pattern): Restore stock in MongoDB if the SQL transaction fails
            reservedItems.forEach(productService::releaseStock);
            throw ex;
        }
    }

    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        // DELETE idempotency: if already cancelled, return without an error
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Paid orders cannot be cancelled directly.");
        }

        transactionTemplate.executeWithoutResult(status -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        });

        // Restore stock to the NoSQL catalog
        order.getItems().forEach(item ->
                productService.releaseStock(item.getProductId(), item.getQuantity()));
    }
}