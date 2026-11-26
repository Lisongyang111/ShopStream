package com.shopstream.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(ProductEventListener.class);

    private final ProductSearchRepository repository;

    public ProductEventListener(ProductSearchRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(
            topics = "${app.topics.product-events}",
            groupId = "search-service"
    )
    public void handle(ProductEvent event) {
        if (event.productId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }

        switch (event.eventType()) {
            case "ProductCreated", "ProductUpdated" -> {
                ProductDocument document = new ProductDocument();

                document.setId(event.productId());
                document.setName(event.name());
                document.setDescription(event.description());
                document.setCategory(event.category());
                document.setPrice(
                        event.price() == null
                                ? null
                                : event.price().doubleValue()
                );
                document.setStock(event.stock());

                repository.save(document);

                log.info(
                        "Indexed product {} after {}",
                        event.productId(),
                        event.eventType()
                );
            }

            case "ProductDeleted" -> {
                repository.deleteById(event.productId());

                log.info(
                        "Deleted product {} from search index",
                        event.productId()
                );
            }

            default -> log.warn(
                    "Ignored unknown product event: {}",
                    event.eventType()
            );
        }
    }
}