package com.nimbleways.springboilerplate.services.order.seasonal;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.notification.DelayNotifier;
import com.nimbleways.springboilerplate.services.notification.NotificationService;
import com.nimbleways.springboilerplate.services.order.OrderProductProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SeasonalProductOrderProcessor implements OrderProductProcessor {
    @Override
    public ProductType getProductType() {
        return ProductType.SEASONAL;
    }


    private final ProductRepository productRepository;
    private final DelayNotifier delayNotifier;
    private final NotificationService notificationService;

    public SeasonalProductOrderProcessor(ProductRepository productRepository , DelayNotifier delayNotifier , NotificationService notificationService) {
        this.productRepository = productRepository;
        this.delayNotifier = delayNotifier;
        this.notificationService = notificationService;
    }

    @Override
    public void process(Product product) {

        if (product.isInSeason() && product.isProductAvailiable()) {
            product.decrementStock();
            productRepository.save(product);
            return;
        }

        if (product.isOutOfSeasonAfterDelay()) {
            notificationService.sendOutOfStockNotification(product.getName());
            product.markOutOfStock();
            productRepository.save(product);
            return;
        }

        if (product.seanonNotStarted()) {
            notificationService.sendOutOfStockNotification(product.getName());
            return;
        }

        delayNotifier.notifyDelay(product.getLeadTime(), product);
    }
}
