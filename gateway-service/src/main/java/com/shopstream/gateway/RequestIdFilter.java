package com.shopstream.gateway;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(RequestIdFilter.class);

    private static final String HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String suppliedId = exchange.getRequest()
                .getHeaders()
                .getFirst(HEADER);

        String requestId =
                suppliedId == null || suppliedId.isBlank()
                        ? UUID.randomUUID().toString()
                        : suppliedId;

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(HEADER, requestId))
                .build();

        exchange.getResponse()
                .getHeaders()
                .set(HEADER, requestId);

        log.info(
                "Request {}: {} {}",
                requestId,
                request.getMethod(),
                request.getURI().getPath()
        );

        return chain.filter(
                exchange.mutate().request(request).build()
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}