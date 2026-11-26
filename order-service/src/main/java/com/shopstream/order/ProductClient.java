package com.shopstream.order;

import feign.FeignException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProductClient {

    private final ProductFeignClient client;

    public ProductClient(ProductFeignClient client) {
        this.client = client;
    }

    public ProductResponse findById(Long productId) {
        ProductResponse product;

        try {
            product = client.findById(productId);

        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product does not exist: " + productId
            );

        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to retrieve product information",
                    exception
            );
        }

        if (product == null || product.price() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Product service returned no price"
            );
        }

        return product;
    }
}