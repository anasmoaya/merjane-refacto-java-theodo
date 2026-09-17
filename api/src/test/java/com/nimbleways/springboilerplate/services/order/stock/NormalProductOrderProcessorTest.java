package com.nimbleways.springboilerplate.services.order.stock;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.notification.DelayNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NormalProductOrderProcessorTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    DelayNotifier notifier;
    NormalProductOrderProcessor normalProductOrderProcessor;


    @BeforeEach
    void setUp() {
        normalProductOrderProcessor = new NormalProductOrderProcessor(productRepository , notifier);
    }

    @Test
    void NormalProduct_isAvailiable_should_decrementStock_and_save(){
        //arrange
        Product product = new Product(1L,1,1, ProductType.NORMAL,
                "product2",null,null,null);
        //act
        normalProductOrderProcessor.process(product);
        //assert
        Mockito.verify(productRepository, times(1)).save(product);
        assertThat(product.getAvailable()).isEqualTo(0);
    }
    @Test
    void NormalProduct_isANotAvailiable_and_HAVELEAD_time_should_notify(){
        //arrange
        Product product = new Product(1L,1,0, ProductType.NORMAL,
                "product2",null,null,null);
        //act
        normalProductOrderProcessor.process(product);
        //assert
        Mockito.verify(notifier, times(1)).notifyDelay(product.getLeadTime(),product);

    }
    @Test
    void product_Not_availiable_with_no_Lead_time(){
        Product product = new Product(1L,0,0, ProductType.NORMAL,
                "product2",null,null,null);
        //act
        normalProductOrderProcessor.process(product);
        //assert
        verifyNoInteractions(notifier);
        verifyNoInteractions(productRepository);
        assertThat(product.getAvailable()).isZero();
    }
}