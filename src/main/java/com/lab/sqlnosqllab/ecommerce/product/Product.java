package com.lab.sqlnosqllab.ecommerce.product;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
@CompoundIndex(name = "category_price_idx", def = "{'category': 1, 'price': 1}")
@Getter
@Setter
public class Product {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sku;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stock;

    // Flexible schema: each product can have different properties
    private Map<String, Object> attributes;

    // Controlled denormalization: reviews embedded within the document
    private List<Review> reviews = new ArrayList<>();

    public record Review(String userName, int rating, String comment) {
    }

    // Getters and setters omitted for brevity (use Lombok or generate them in the IDE)
}