package com.nimbleways.springboilerplate.services.order.expirable;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.notification.NotificationService;
import com.nimbleways.springboilerplate.services.order.OrderProductProcessor;
import org.springframework.stereotype.Component;



@Component
public class ExpirableProductOrderProcessor implements OrderProductProcessor {
    private final ProductRepository repository;
    private final NotificationService notificationService;

    public ExpirableProductOrderProcessor(ProductRepository repository, NotificationService notificationService ) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Override
    public ProductType getProductType() {
        return ProductType.EXPIRABLE;
    }

    @Override
    public void process(Product product) {
        if (product.isProductAvailiable() && !product.isExpired()) {
            product.decrementStock();
            repository.save(product);
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.markOutOfStock();
            repository.save(product);
        }
    }


}

