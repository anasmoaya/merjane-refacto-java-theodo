package com.nimbleways.springboilerplate.services.order.expirable;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.services.order.OrderProductProcessor;
import org.springframework.stereotype.Component;

@Component
public class ExpirableProductOrderProcessor implements OrderProductProcessor {
    @Override
    public ProductType getProductType() {
        return ProductType.EXPIRABLE;
    }

    @Override
    public void process(Product product) {

    }
}
