package com.example.productsMS.resources.controller;


import com.example.productsMS.domain.Product;
import com.example.productsMS.services.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Product>> listaProdutos (){

        List<Product> response = service.listarProdutos();
        return ResponseEntity.ok(response);

    }

}
