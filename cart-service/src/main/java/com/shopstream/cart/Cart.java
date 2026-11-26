package com.shopstream.cart;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@RedisHash("carts")
public class Cart {

    @Id
    private String userId;

    private List<CartItem> items = new ArrayList<>();

    private Long checkoutOrderId;

    @TimeToLive
    private Long timeToLive = 86400L;

    public Cart() {
    }

    public Cart(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Long getCheckoutOrderId() {
        return checkoutOrderId;
    }

    public void setCheckoutOrderId(Long checkoutOrderId) {
        this.checkoutOrderId = checkoutOrderId;
    }

    public Long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Long timeToLive) {
        this.timeToLive = timeToLive;
    }
}