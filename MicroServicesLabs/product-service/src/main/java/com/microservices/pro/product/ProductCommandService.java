package com.microservices.pro.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductCommandService {

    private static final Logger log = LoggerFactory.getLogger(ProductCommandService.class);

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductCommandService(ProductRepository productRepository, ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Product create(Product request) {
        Product product = new Product(null, request.getName(), request.getDescription(),
                request.getPrice(), request.getCategory());
        validatePrice(product);
        Product saved = productRepository.save(product);
        publish(saved.getId(), "CREATED");
        return saved;
    }

    @Transactional
    public Product update(Long id, Product product) {
        product.setId(id);
        validatePrice(product);
        Product saved = productRepository.save(product);
        publish(saved.getId(), "UPDATED");
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        productRepository.deleteById(id);
        publish(id, "DELETED");
    }

    private void validatePrice(Product product) {
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductException("Price must be positive");
        }
    }

    private void publish(Long productId, String changeType) {
        eventPublisher.publishEvent(new ProductChangedEvent(productId, changeType));
        log.info("[CQRS] ProductChangedEvent published: productId={}, changeType={}", productId, changeType);
    }
}
