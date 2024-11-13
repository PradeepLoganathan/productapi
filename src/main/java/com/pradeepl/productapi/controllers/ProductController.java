// src/main/java/com/example/productapi/controllers/ProductController.java
package com.pradeepl.productapi.controllers;

import com.pradeepl.productapi.models.Product;
import com.pradeepl.productapi.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;



@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*") // Enable CORS for all origins (for testing, adjust as needed)
public class ProductController {

    @Autowired
    private ProductRepository repository;

    // GET all products
    @GetMapping
    public List<Product> getAllProducts() {
        System.out.println("Thread Name: " + Thread.currentThread().getName());
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                Thread.currentThread().setName("VirtualThread-"  );
                System.out.println("Running task in a virtual thread: " 
                                   + Thread.currentThread().getName());
            });
        }

        executor.shutdown();
        try {
            // Wait for all tasks to complete or timeout after 1 minute
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                System.err.println("Tasks did not finish within the timeout.");
            }
        } catch (InterruptedException e) {
            // Handle the interruption
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return repository.findAll();
    }

    // GET a specific product by ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable int id) {
        System.out.println("Thread Name: " + Thread.currentThread().getName());
        Optional<Product> product = repository.findById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST a new product
    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return repository.save(product);
    }

    // DELETE a product by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable int id) {
        System.out.println("Thread Name: " + Thread.currentThread().getName());
        Optional<Product> product = repository.findById(id);
        if (product.isPresent()) {
            repository.delete(product.get());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
