package com.lab.sqlnosqllab.ecommerce.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findBySku(String sku);

    // Projection: filters by category and excludes the 'reviews' array from the response
    @Query(value = "{ 'category': ?0 }", fields = "{ 'reviews': 0 }")
    Page<Product> findByCategory(String category, Pageable pageable);

    // Projection: retrieves all products without loading 'reviews'
    @Query(value = "{}", fields = "{ 'reviews': 0 }")
    Page<Product> findAllProjectedBy(Pageable pageable);

}
