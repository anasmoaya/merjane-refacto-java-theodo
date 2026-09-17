package com.nimbleways.springboilerplate.services.order.seasonal;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.services.order.OrderProductProcessor;
import org.springframework.stereotype.Component;

@Component
public class SeasonalProductOrderProcessor implements OrderProductProcessor {
    @Override
    public ProductType getProductType() {
        return ProductType.SEASONAL;
    }

    @Override
    public void process(Product product) {

    }
}
