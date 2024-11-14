package com.pradeepl.productapi.controllers;

import com.pradeepl.productapi.models.Product;
import com.pradeepl.productapi.models.Product.ProductRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import java.util.List;
import java.util.SequencedCollection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@CrossOrigin("*")
 // For CORS
public class VulnerableProductController {

    @Autowired
    private DataSource dataSource; // Inject the DataSource
    // Define a fixed thread pool with a limited number of threads
    //private final ExecutorService executor = Executors.newFixedThreadPool(10);
     // Executor with unlimited thread pool size (potential DoS vulnerability)
     private final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping("/products/vulnerable/{name}")
    public ResponseEntity<List<Product>> getProductsByName(@PathVariable String name) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery
                ("SELECT * FROM product WHERE name = '" + name + "'")) {

                    List<Product> products = new ArrayList<>();
                    while (resultSet.next()) {
                        Product product = new Product();                        
                        product.setName(resultSet.getString("name"));
                        product.setPrice(resultSet.getDouble("price"));
                        products.add(product);
            }
            return ResponseEntity.ok(products);

        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
//#region JEP440
    //the vuln CWE-79 is not identified in this method but is identified in the one below it
    @PostMapping("/products/vulnerable440")
    public ResponseEntity<String> testProduct21(@RequestBody ProductRecord productRecord) {
        switch (productRecord) {
            case ProductRecord(int id, String name, double price) -> {
                // Vulnerable: potential injection
                // This part is vulnerable since 'name' is not sanitized
                return ResponseEntity.ok("Processing product: " + name); 
            }
            default -> {
                return ResponseEntity.ok("Product processed.");
            }
        }
    }

    @PostMapping("/products/vulnerabletest") // New endpoint for demonstration
    public ResponseEntity<String> testProduct17(@RequestBody ProductRecord productRecord) {
        return ResponseEntity.ok("Processing product: " + productRecord.name()); 
    }
//#endregion

//#region JEP-430
    @PostMapping("/products/vulnerable430")
    public ResponseEntity<String> testProduct430(@RequestBody ProductRecord productRecord) {
        // Assuming JEP 430 String Templates with SQL-like query construction
        String productName = productRecord.name();
        
        String sqlQuery = "SELECT * FROM products WHERE name = ${productName}";
        
        return ResponseEntity.ok("Generated query: " + sqlQuery);
    }
//#endregion

//#region JEP-431

    @PostMapping("/products/processProducts")
    public ResponseEntity<String> processProducts(@RequestBody List<ProductRecord> productRecords) {
    
    StringBuilder response = new StringBuilder("Processing products:<br>");
    
    // Vulnerable section: directly embedding user input without sanitization
    for (ProductRecord productRecord : productRecords) {
        response.append("<p>Product ID: ")
                .append(productRecord.id())
                .append(", Name: ")
                .append(productRecord.name())  // Potential XSS vulnerability here
                .append(", Price: ")
                .append(productRecord.price())
                .append("</p>");
    }

        return ResponseEntity.ok(response.toString());
    }

    //JEP-431 -  This feature adds a collection that maintains a defined order for retrieval in both directions (from start to end and vice versa).
    @PostMapping("/products/sequencedCollectionExample")
    public ResponseEntity<String> processProducts21(@RequestBody List<ProductRecord> productRecords) {
        
        // Using a SequencedCollection to process items in defined order
        SequencedCollection<ProductRecord> sequencedProducts = List.copyOf(productRecords);
        
        StringBuilder response = new StringBuilder("Processing products in order:\n");
        
        // Process each product in forward order
        sequencedProducts.forEach(productRecord -> 
            response.append("Product ID: ")
                    .append(productRecord.id())
                    .append(", Name: ")
                    .append(productRecord.name())
                    .append(", Price: ")
                    .append(productRecord.price())
                    .append("\n")
        );
        
        // Additional processing in reverse order, if needed
        response.append("\nProcessing products in reverse order:\n");
        sequencedProducts.reversed().forEach(productRecord -> 
            response.append("Product ID: ")
                    .append(productRecord.id())
                    .append(", Name: ")
                    .append(productRecord.name())
                    .append(", Price: ")
                    .append(productRecord.price())
                    .append("\n")
        );
        
        return ResponseEntity.ok(response.toString());
    }
//#endregion

//#region Virtual Threads (JEP 444)
    @PostMapping("/products/virtualThreadsExample")
    public ResponseEntity<String> processProductsVirtThread(@RequestBody List<ProductRecord> productRecords) {
        
        // Creating a virtual thread executor
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        
        StringBuilder response = new StringBuilder("Processing products concurrently using virtual threads:<br>");
        
        try {
            // Use virtual threads to process each product concurrently
            List<Future<String>> futures = productRecords.stream()
                    .map(productRecord -> executor.submit(() -> processProductVirtThread(productRecord)))
                    .toList();

            // Collect results from each virtual thread
            for (Future<String> future : futures) {
                response.append(future.get()).append("<br>");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing products: " + e.getMessage());
        } finally {
            executor.shutdown(); // Always shut down the executor
        }

        return ResponseEntity.ok(response.toString());
    }

    // Helper method to simulate product processing
    private String processProductVirtThread(ProductRecord productRecord) {
        // Simulate some processing for each product
        return "Product ID: " + productRecord.id() +
            ", Name: " + productRecord.name() +
            ", Price: " + productRecord.price();
    }

    //java17
    @PostMapping("/products/regularThreadsExample")
    public ResponseEntity<String> processProductsRegThread(@RequestBody List<ProductRecord> productRecords) {
        
        StringBuilder response = new StringBuilder("Processing products concurrently using regular threads:<br>");

        try {
            // Submit each product processing task to the fixed thread pool
            List<Future<String>> futures = productRecords.stream()
                    .map(productRecord -> executor.submit(() -> processProductRegularThread(productRecord)))
                    .toList();

            // Collect results from each thread
            for (Future<String> future : futures) {
                response.append(future.get()).append("<br>");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            return ResponseEntity.status(500).body("Request interrupted while processing.");
        } catch (Exception e) {
            // Log error without exposing details to the client
            e.printStackTrace();
            return ResponseEntity.status(500).body("An error occurred while processing the request.");
        }

        return ResponseEntity.ok(response.toString());
    }

    // Helper method to simulate product processing
    private String processProductRegularThread(ProductRecord productRecord) {
        // Simulate some processing for each product
        return "Product ID: " + productRecord.id() +
               ", Name: " + productRecord.name() +
               ", Price: " + productRecord.price();
    }
//#endregion



}