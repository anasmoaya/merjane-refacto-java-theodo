package com.nimbleways.springboilerplate.services;

import java.time.LocalDate;

import com.nimbleways.springboilerplate.services.notification.DelayNotifier;
import com.nimbleways.springboilerplate.services.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
public class ProductService {

    @Autowired
    ProductRepository pr;

    @Autowired
    NotificationService ns;

    @Autowired
    DelayNotifier delayNotifier;



    public void handleSeasonalProduct(Product p) {
        if (LocalDate.now().plusDays(p.getLeadTime()).isAfter(p.getSeasonEndDate())) {
            ns.sendOutOfStockNotification(p.getName());
            p.setAvailable(0);
            pr.save(p);
        } else if (p.getSeasonStartDate().isAfter(LocalDate.now())) {
            ns.sendOutOfStockNotification(p.getName());
            pr.save(p);
        } else {
            delayNotifier.notifyDelay(p.getLeadTime(), p);
        }
    }

    public void handleExpiredProduct(Product p) {
        if (p.getAvailable() > 0 && p.getExpiryDate().isAfter(LocalDate.now())) {
            p.setAvailable(p.getAvailable() - 1);
            pr.save(p);
        } else {
            ns.sendExpirationNotification(p.getName(), p.getExpiryDate());
            p.setAvailable(0);
            pr.save(p);
        }
    }
}