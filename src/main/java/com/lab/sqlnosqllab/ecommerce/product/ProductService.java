package com.lab.sqlnosqllab.ecommerce.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final MongoTemplate mongoTemplate;

    public ProductService(ProductRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    public Product create(Product product) {
        return repository.save(product);
    }

    public Page<Product> findAll(String category, Pageable pageable) {
        if (category != null && !category.isBlank()) {
            return repository.findByCategory(category, pageable);
        }
        return repository.findAll(pageable);
    }

    // Atomic stock increment for the PATCH endpoint
    public Product addStock(String productId, int quantityToAdd) {
        Query query = new Query(Criteria.where("_id").is(productId));
        Update update = new Update().inc("stock", quantityToAdd);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

        Product updated = mongoTemplate.findAndModify(query, update, options, Product.class);
        if (updated == null) {
            throw new java.util.NoSuchElementException("Product not found: " + productId);
        }
        return updated;
    }

    // Atomic decrement: only executes if stock >= quantity
    public Product reserveStock(String productId, int quantity) {
        Query query = new Query(
                Criteria.where("_id").is(productId).and("stock").gte(quantity)
        );
        Update update = new Update().inc("stock", -quantity);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

        Product updated = mongoTemplate.findAndModify(query, update, options, Product.class);
        if (updated == null) {
            throw new IllegalStateException(
                    "Product not found or insufficient stock: " + productId
            );
        }
        return updated;
    }

    // Compensating action to restore stock
    public void releaseStock(String productId, int quantity) {
        Query query = new Query(Criteria.where("_id").is(productId));
        Update update = new Update().inc("stock", quantity);
        mongoTemplate.updateFirst(query, update, Product.class);
    }
}