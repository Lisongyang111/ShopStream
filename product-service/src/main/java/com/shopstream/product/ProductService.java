package com.shopstream.product;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(
            ProductRepository repository,
            ApplicationEventPublisher eventPublisher) {

        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    @Cacheable(cacheNames = "products", key = "#p0")
    public Product findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));
    }

    @Transactional
    public Product create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);

        Product saved = repository.save(product);
        publishEvent("ProductCreated", saved);

        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = "products", key = "#p0")
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);
        applyRequest(product, request);

        Product saved = repository.save(product);
        publishEvent("ProductUpdated", saved);

        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = "products", key = "#p0")
    public void delete(Long id) {
        Product product = findById(id);

        repository.delete(product);
        publishEvent("ProductDeleted", product);
    }

    private void publishEvent(String eventType, Product product) {
        ProductEvent event = new ProductEvent(
                eventType,
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getStock()
        );

        eventPublisher.publishEvent(event);
    }

    private void applyRequest(
            Product product,
            ProductRequest request) {

        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setStock(request.stock());
    }
}