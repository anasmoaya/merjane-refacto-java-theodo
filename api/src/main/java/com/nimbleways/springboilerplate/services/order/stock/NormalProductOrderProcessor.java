package com.nimbleways.springboilerplate.services.order.stock;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.notification.DelayNotifier;
import com.nimbleways.springboilerplate.services.order.OrderProductProcessor;
import org.springframework.stereotype.Component;

@Component
public class NormalProductOrderProcessor implements OrderProductProcessor {
    private final ProductRepository productRepository;
    private final DelayNotifier delayNotifier;

    public NormalProductOrderProcessor(ProductRepository productRepository, DelayNotifier delayNotifier) {
        this.productRepository = productRepository;
        this.delayNotifier = delayNotifier;
    }

    @Override
    public ProductType getProductType() {
        return ProductType.NORMAL;
    }

    @Override
    public void process(Product product) {

        if (product.getAvailable() > 0) {
            product.decrementStock();
            productRepository.save(product);
        } else {
            if (product.hasLeadTime()) {
                delayNotifier.notifyDelay(product.getLeadTime(), product);
            }
        }

    }
}
