package com.example.productsMS.services;

import com.example.productsMS.domain.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final List<Product> products = new ArrayList<Product>(List.of(
            new Product(1, "Notebook Lenovo IdeaPad", "Notebook com 16GB de RAM e SSD de 512GB", 3499.90, 12),
            new Product(2, "Mouse Logitech G203", "Mouse gamer com sensor óptico e iluminação RGB", 149.90, 35),
            new Product(3, "Teclado Mecânico Redragon", "Teclado mecânico com switches táteis e iluminação RGB", 279.90, 18),
            new Product(4, "Monitor LG 24 polegadas", "Monitor Full HD com painel IPS de 100Hz", 699.90, 9),
            new Product(5, "Headset HyperX Cloud", "Headset com microfone integrado e conexão USB", 399.90, 14),
            new Product(6, "SSD Kingston 1TB", "SSD NVMe M.2 com capacidade de 1TB", 459.90, 22),
            new Product(7, "Memória RAM Kingston 16GB", "Módulo de memória DDR4 com frequência de 3200MHz", 189.90, 27),
            new Product(8, "Webcam Logitech C920", "Webcam Full HD 1080p com microfone estéreo", 349.90, 7),
            new Product(9, "Caixa de Som JBL", "Caixa de som Bluetooth portátil", 229.90, 16),
            new Product(10, "Carregador USB-C", "Carregador de 65W com suporte a Power Delivery", 119.90, 40)
    ));

    public List<Product> listarProdutos(){
        return this.products;
    }

}
