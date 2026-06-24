package com.microservices.pro.product;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<Product> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Product save(Product product) {
        long id = idGenerator.getAndIncrement();
        Product saved = new Product(id, product.name(), product.description(), product.price(), product.category());
        store.put(id, saved);
        return saved;
    }

    public void deleteById(Long id) {
        store.remove(id);
    }
}