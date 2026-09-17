

package com.nimbleways.springboilerplate.services.order.seasonal;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.notification.NotificationService;
import com.nimbleways.springboilerplate.services.notification.DelayNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.management.Notification;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SeasonalProductOrderProcessorTest {

    @Mock
    private  ProductRepository productRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private DelayNotifier delayNotifier;

    @InjectMocks SeasonalProductOrderProcessor seasonalProductOrderProcessor;


    @BeforeEach
    void setUp(){
        seasonalProductOrderProcessor = new SeasonalProductOrderProcessor(productRepository,delayNotifier ,notificationService );
    }

    @Test
    void product_inSeason_and_availiable_should_decrement_qty(){
        Product product  = new Product(1L,4,4, ProductType.EXPIRABLE ,
                "prod", null,LocalDate.now().minusMonths(1) ,LocalDate.now().plusMonths(1) );

        seasonalProductOrderProcessor.process(product);
        assertThat(product.getAvailable()).isEqualTo(3);
        verify(productRepository,times(1)).save(product);

    }

    @ParameterizedTest
    @MethodSource("notAvailiableOrNotInseasonAndOutOfSeasonAfterDelay")
    void not_Availiable_Or_NotInseason_AndOut_OfSeason_After_Delay(Product product){
        seasonalProductOrderProcessor.process(product);

        verify(notificationService, times(1)).sendOutOfStockNotification(product.getName());
        verify(productRepository, times(1)).save(product);
        verifyNoInteractions(delayNotifier);
        assertThat(product.getAvailable()).isEqualTo(0);
    }

    static Stream<Arguments> notAvailiableOrNotInseasonAndOutOfSeasonAfterDelay(){
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(1L, 4, 0, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.plusDays(1))),
                Arguments.of(new Product(2L, 4, 4, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.minusDays(1))),
                Arguments.of(new Product(3L, 4, 0, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.minusDays(1))),
                Arguments.of(new Product(4L, 0, 0, ProductType.SEASONAL,
                                "prod", null, today.minusMonths(1), today.minusDays(1)),
                Arguments.of(new Product(5L, 10, 0, ProductType.SEASONAL,
                                "prod", null, today.plusDays(1), today.plusDays(3)))));
    }

    @ParameterizedTest
    @MethodSource("seasonNotStartedYet")
    void season_Not_Started_Should_Notify_OutOfStock_Without_Changing_Stock(Product product){
        int availableBefore = product.getAvailable();

        seasonalProductOrderProcessor.process(product);

        verify(notificationService, times(1)).sendOutOfStockNotification(product.getName());
        verifyNoInteractions(delayNotifier);
        assertThat(product.getAvailable()).isEqualTo(availableBefore);
    }

    static Stream<Arguments> seasonNotStartedYet(){
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(6L, 4, 4, ProductType.SEASONAL,
                        "prod", null, today.plusDays(5), today.plusMonths(1))),
                Arguments.of(new Product(7L, 4, 0, ProductType.SEASONAL,
                                "prod", null, today.plusDays(5), today.plusMonths(1))),
                Arguments.of(new Product(8L, 0, 4, ProductType.SEASONAL,
                                "prod", null, today.plusDays(1), today.plusMonths(1)))
        );
    }

    @ParameterizedTest
    @MethodSource("inSeasonNotAvailableRestockBeforeSeasonEnd")
    void in_Season_Not_Available_Restock_Before_Season_End_Should_Notify_Delay(Product product){
        int leadTime = product.getLeadTime();

        seasonalProductOrderProcessor.process(product);

        verify(delayNotifier, times(1)).notifyDelay(leadTime, product);
        verifyNoInteractions(notificationService);
        verifyNoInteractions(productRepository);
        assertThat(product.getAvailable()).isEqualTo(0);
    }

    static Stream<Arguments> inSeasonNotAvailableRestockBeforeSeasonEnd(){
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(9L, 4, 0, ProductType.SEASONAL,
                                "prod", null, today.minusMonths(1), today.plusMonths(1))),
                Arguments.of(new Product(10L, 0, 0, ProductType.SEASONAL,
                                "prod", null, today.minusMonths(1), today.plusMonths(1))),
                Arguments.of(new Product(11L, 4, 0, ProductType.SEASONAL,
                                "prod", null, today.minusMonths(1), today.plusDays(4)))
        );
    }



}