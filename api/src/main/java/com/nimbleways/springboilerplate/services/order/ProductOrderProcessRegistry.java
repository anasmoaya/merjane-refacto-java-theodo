package com.nimbleways.springboilerplate.services.order;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProductOrderProcessRegistry {

    private final Map<ProductType, OrderProductProcessor> processorMap = new EnumMap<>(ProductType.class);


    public ProductOrderProcessRegistry(List<OrderProductProcessor> processorList){
        processorList.forEach(p->processorMap.put(p.getProductType() , p));
    }


    public void process(Product product){
        OrderProductProcessor productOrderProcessor = processorMap.get(product.getType());
        productOrderProcessor.process(product);
    }

}