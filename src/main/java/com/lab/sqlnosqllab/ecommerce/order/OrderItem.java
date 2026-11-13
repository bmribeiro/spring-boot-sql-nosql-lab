package com.lab.sqlnosqllab.ecommerce.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private String productId; // Logical reference to the MongoDB ObjectId

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice; // Snapshot of the price at the time of purchase

    @Column(nullable = false)
    private Integer quantity;
}