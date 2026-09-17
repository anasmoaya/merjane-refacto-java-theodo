package com.nimbleways.springboilerplate.services.order;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;

public interface OrderProductProcessor {

    ProductType getProductType();
    void process(Product product);
}
