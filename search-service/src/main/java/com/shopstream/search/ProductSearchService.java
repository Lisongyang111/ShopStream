package com.shopstream.search;

import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductSearchService {

    private final ProductSearchRepository repository;
    private final ElasticsearchOperations operations;

    public ProductSearchService(
            ProductSearchRepository repository,
            ElasticsearchOperations operations) {

        this.repository = repository;
        this.operations = operations;
    }

    public Page<ProductDocument> search(
            String q,
            String category,
            Double minPrice,
            Double maxPrice,
            int page,
            int size) {

        if (page < 0 || size < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be positive"
            );
        }

        if (minPrice != null && !Double.isFinite(minPrice)
                || maxPrice != null && !Double.isFinite(maxPrice)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Price limits must be finite numbers"
            );
        }

        if (minPrice != null && maxPrice != null
                && minPrice > maxPrice) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum price cannot exceed maximum price"
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        boolean hasText = q != null && !q.isBlank();
        boolean hasCategory =
                category != null && !category.isBlank();

        if (!hasText && minPrice == null && maxPrice == null) {
            if (hasCategory) {
                return repository.findByCategory(
                        category.trim(),
                        pageable
                );
            }

            return repository.findAll(pageable);
        }

        BoolQuery.Builder conditions = new BoolQuery.Builder();

        if (hasText) {
            conditions.must(query -> query.multiMatch(match -> match
                    .fields("name", "description")
                    .query(q.trim())
            ));
        }

        if (hasCategory) {
            conditions.filter(query -> query.term(term -> term
                    .field("category")
                    .value(category.trim())
            ));
        }

        if (minPrice != null || maxPrice != null) {
            NumberRangeQuery.Builder priceRange =
                    new NumberRangeQuery.Builder().field("price");

            if (minPrice != null) {
                priceRange.gte(minPrice);
            }

            if (maxPrice != null) {
                priceRange.lte(maxPrice);
            }

            conditions.filter(query -> query.range(range -> range
                    .number(priceRange.build())
            ));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(builder -> builder.bool(conditions.build()))
                .withPageable(pageable)
                .withTrackTotalHits(true)
                .build();

        SearchHits<ProductDocument> hits =
                operations.search(query, ProductDocument.class);

        List<ProductDocument> products = hits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(
                products,
                pageable,
                hits.getTotalHits()
        );
    }
}