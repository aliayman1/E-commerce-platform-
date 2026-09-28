package com.microservices.pro.product;

import java.io.Serializable;
import java.math.BigDecimal;

// A record (DTO projection) rather than the deck's interface projection: the
// query side is @Cacheable into Redis with JDK serialization, and Spring Data's
// interface-projection proxies are not Serializable.
public record ProductSummaryProjection(Long id, String name, BigDecimal price, String categoryName)
        implements Serializable {

    public String getDisplayLabel() {
        return name + " (" + categoryName + ")";
    }
}
