package com.shopstream.search;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final ProductSearchService searchService;

    public SearchController(ProductSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public Page<ProductDocument> search(
            @RequestParam(name = "q", required = false)
            String q,

            @RequestParam(name = "category", required = false)
            String category,

            @RequestParam(name = "minPrice", required = false)
            Double minPrice,

            @RequestParam(name = "maxPrice", required = false)
            Double maxPrice,

            @RequestParam(name = "page", defaultValue = "0")
            int page,

            @RequestParam(name = "size", defaultValue = "10")
            int size) {

        return searchService.search(
                q,
                category,
                minPrice,
                maxPrice,
                page,
                size
        );
    }
}