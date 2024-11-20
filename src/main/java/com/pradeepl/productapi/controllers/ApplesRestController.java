package com.pradeepl.productapi.controllers;

import com.pradeepl.productapi.models.Product;
import com.pradeepl.productapi.repositories.ProductRepository;
import org.springframework.web.bind.annotation.*;
import com.pradeepl.productapi.DatabaseService;
import org.webjars.NotFoundException;

import java.util.List;
import java.util.Optional;

@RestController
public class ApplesRestController {

    DatabaseService databaseService;
    ProductRepository productRepository;

    @GetMapping("/passord")
    public String getPassword () {
        return databaseService.getAwsRDSkey();
    }

    @GetMapping("/products/{id}")
    public Product one(@PathVariable Integer id) throws Exception {

        return productRepository.findById(id)
                .orElseThrow(() -> new Exception());
    }

    @PutMapping("/products/{id}")
    public Product replaceEmployee(@RequestBody Product newProduct, @PathVariable Integer id) {

        return productRepository.findById(id)
                .map(employee -> {
                    employee.setName(newProduct.getName());
                    employee.setPrice(newProduct.getPrice());
                    return productRepository.save(employee);
                })
                .orElseGet(() -> {
                    return productRepository.save(newProduct);
                });
    }

    @GetMapping("/products")
    public List<Product> all() {
        return productRepository.findAll();
    }

}
