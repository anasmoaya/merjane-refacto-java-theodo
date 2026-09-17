package com.nimbleways.springboilerplate.services;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.services.order.ProductOrderProcessRegistry;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import groovy.util.logging.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

import javax.management.Notification;

@Slf4j
@Service
@Log4j
public class ProductService {



    private final OrderRepository orderRepository ;
    private final ProductOrderProcessRegistry productOrderProcessRegistry;

    public ProductService(OrderRepository orderRepository, ProductOrderProcessRegistry productOrderProcessRegistry) {
        this.orderRepository = orderRepository;
        this.productOrderProcessRegistry = productOrderProcessRegistry;
    }



    public ProcessOrderResponse procesOrder(Long orderId)  {
        Order order = orderRepository.findById(orderId).get();  //null check will change the api behavior
        Set<Product> products = order.getItems();
        products.forEach(productOrderProcessRegistry::process);
        return new ProcessOrderResponse(order.getId());
    }



}