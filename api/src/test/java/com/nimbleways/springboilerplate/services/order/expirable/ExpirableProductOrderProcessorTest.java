package com.nimbleways.springboilerplate.services.order.expirable;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;


@ExtendWith(MockitoExtension.class)
class ExpirabaleProductOrderProcessorTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    NotificationService notificationService;

    ExpirableProductOrderProcessor expirabaleProductOrderProcessor;

    @BeforeEach
    void setUp(){
        expirabaleProductOrderProcessor = new ExpirableProductOrderProcessor(productRepository ,notificationService);
    }


    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("availableAndNotExpired")
    void available_and_not_expired_should_decrement_and_save(Product product, String scenario) {
        int availableBefore = product.getAvailable();

        expirabaleProductOrderProcessor.process(product);

        verify(productRepository, times(1)).save(product);
        verifyNoInteractions(notificationService);
        assertThat(product.getAvailable()).isEqualTo(availableBefore - 1);
    }



    @ParameterizedTest
    @MethodSource("expiredOrNotAvailable")
    void expired_or_not_available_should_notify_and_mark_out_of_stock(Product product, String scenario) {
        expirabaleProductOrderProcessor.process(product);

        verify(notificationService, times(1)).sendExpirationNotification(product.getName(), product.getExpiryDate());
        verify(productRepository, times(1)).save(product);
        assertThat(product.getAvailable()).isEqualTo(0);
    }

    static Stream<Arguments> expiredOrNotAvailable(){
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(5L, 10, 0, ProductType.EXPIRABLE,
                                "prod", today.minusMonths(1), null, null)),
                Arguments.of(new Product(6L, 10, 5, ProductType.EXPIRABLE,
                                "prod", today.minusMonths(1), null, null)),
                // boundary: expired yesterday
                Arguments.of(new Product(7L, 10, 5, ProductType.EXPIRABLE,
                                "prod", today.minusDays(1), null, null)),
                Arguments.of(new Product(8L, 10, 0, ProductType.EXPIRABLE,
                                "prod", today.plusMonths(1), null, null)),
                Arguments.of(new Product(9L, 10, 0, ProductType.EXPIRABLE,
                                "prod", today, null, null)
        ));
    }
    static Stream<Arguments> availableAndNotExpired(){
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(1L, 10, 1, ProductType.EXPIRABLE,
                                "prod", today.plusMonths(1), null, null),
                        Arguments.of(new Product(2L, 10, 5, ProductType.EXPIRABLE,
                                "prod", today.plusDays(1), null, null)),
                        Arguments.of(new Product(3L, 10, 3, ProductType.EXPIRABLE,
                                "prod", today, null, null)),
                        Arguments.of(new Product(4L, 0, 2, ProductType.EXPIRABLE,
                                "prod", today.plusYears(1), null, null)
                        )
                )
        );
    }


}
