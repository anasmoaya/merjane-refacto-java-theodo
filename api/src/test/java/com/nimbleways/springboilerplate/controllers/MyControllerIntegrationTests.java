package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.dto.product.ProductType;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.notification.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MyControllerIntegrationTests {

    private static final String PROCESS_ORDER_URL = "/orders/{orderId}/processOrder";
    private static final String PROVIDERS = "com.nimbleways.springboilerplate.controllers.MyControllerIntegrationTests#";

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }


    private Order saveOrderWith(Product... products) {
        List<Product> saved = productRepository.saveAll(Arrays.asList(products));
        Order order = new Order();
        order.setItems(new HashSet<>(saved));
        return orderRepository.save(order);
    }

    private void processOrder(Order order) throws Exception {
        mockMvc.perform(post(PROCESS_ORDER_URL, order.getId())
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()));
    }

    private Product reload(Product product) {
        return productRepository.findById(product.getId()).orElseThrow();
    }


    static Stream<Arguments> normalAvailable() {
        return Stream.of(
                Arguments.of(new Product(null, 1, 1, ProductType.NORMAL,
                                "product2", null, null, null),
                        "available, last unit")
        );
    }

    static Stream<Arguments> normalNotAvailableWithLeadTime() {
        return Stream.of(
                Arguments.of(new Product(null, 1, 0, ProductType.NORMAL,
                                "product2", null, null, null),
                        "not available, leadTime = 1")
        );
    }

    static Stream<Arguments> normalNotAvailableWithoutLeadTime() {
        return Stream.of(
                Arguments.of(new Product(null, 0, 0, ProductType.NORMAL,
                                "product2", null, null, null),
                        "not available, leadTime = 0")
        );
    }

    static Stream<Arguments> expirableAvailableAndNotExpired() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(null, 10, 1, ProductType.EXPIRABLE,
                        "prod", today.plusMonths(1), null, null)),
                Arguments.of(new Product(null, 10, 5, ProductType.EXPIRABLE,
                        "prod", today.plusDays(1), null, null)),
                // boundary: expiry date == today is not yet expired (isExpired uses isBefore)
                Arguments.of(new Product(null, 10, 3, ProductType.EXPIRABLE,
                        "prod", today, null, null)),
                Arguments.of(new Product(null, 0, 2, ProductType.EXPIRABLE,
                        "prod", today.plusYears(1), null, null))
        );
    }

    static Stream<Arguments> expirableExpiredOrNotAvailable() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(null, 10, 0, ProductType.EXPIRABLE,
                        "prod", today.minusMonths(1), null, null)),
                Arguments.of(new Product(null, 10, 5, ProductType.EXPIRABLE,
                        "prod", today.minusMonths(1), null, null)),
                // boundary: expired yesterday
                Arguments.of(new Product(null, 10, 5, ProductType.EXPIRABLE,
                        "prod", today.minusDays(1), null, null)),
                Arguments.of(new Product(null, 10, 0, ProductType.EXPIRABLE,
                        "prod", today.plusMonths(1), null, null)),
                Arguments.of(new Product(null, 10, 0, ProductType.EXPIRABLE,
                        "prod", today, null, null))
        );
    }

    // SeasonalProductOrderProcessorTest
    static Stream<Arguments> seasonalInSeasonAndAvailable() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(null, 4, 4, ProductType.SEASONAL,
                                "prod", null, today.minusMonths(1), today.plusMonths(1)),
                        "in season, available")
        );
    }

    static Stream<Arguments> seasonalNotAvailableOrNotInSeasonAndOutOfSeasonAfterDelay() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                // not available, still in season, but leadTime pushes the restock past season end
                Arguments.of(new Product(null, 4, 0, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.plusDays(1))),
                // available, but season already ended
                Arguments.of(new Product(null, 4, 4, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.minusDays(1))),
                // not available AND season already ended
                Arguments.of(new Product(null, 4, 0, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.minusDays(1))),
                // not available, season ended, no lead time at all
                Arguments.of(new Product(null, 0, 0, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.minusDays(1))),
                // season not started yet, but leadTime already exceeds season end
                Arguments.of(new Product(null, 10, 0, ProductType.SEASONAL,
                        "prod", null, today.plusDays(1), today.plusDays(3)))
        );
    }

    static Stream<Arguments> seasonalSeasonNotStartedYet() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(null, 4, 4, ProductType.SEASONAL,
                        "prod", null, today.plusDays(5), today.plusMonths(1))),
                Arguments.of(new Product(null, 4, 0, ProductType.SEASONAL,
                        "prod", null, today.plusDays(5), today.plusMonths(1))),
                Arguments.of(new Product(null, 0, 4, ProductType.SEASONAL,
                        "prod", null, today.plusDays(1), today.plusMonths(1)))
        );
    }

    static Stream<Arguments> seasonalInSeasonNotAvailableRestockBeforeSeasonEnd() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(new Product(null, 4, 0, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.plusMonths(1))),
                Arguments.of(new Product(null, 0, 0, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.plusMonths(1))),
                Arguments.of(new Product(null, 4, 0, ProductType.SEASONAL,
                        "prod", null, today.minusMonths(1), today.plusDays(4)))
        );
    }



    @Nested
    @DisplayName("POST /orders/{orderId}/processOrder")
    class Endpoint {

        @Test
        void returns_200_and_order_id_for_an_order_without_items() throws Exception {
            Order order = saveOrderWith();

            processOrder(order);

            verifyNoInteractions(notificationService);
        }

        @Test
        void fails_when_order_does_not_exist() {
            long unknownOrderId = 9999L;

            // no exception handler is registered: the NoSuchElementException from Optional.get()
            // propagates through MockMvc wrapped in a NestedServletException
            assertThatThrownBy(() -> mockMvc.perform(post(PROCESS_ORDER_URL, unknownOrderId)
                    .contentType("application/json")))
                    .isInstanceOf(NestedServletException.class)
                    .hasCauseInstanceOf(NoSuchElementException.class);

            verifyNoInteractions(notificationService);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // NORMAL products
    // ---------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("NORMAL products")
    class NormalProducts {

        @ParameterizedTest
        @MethodSource(PROVIDERS + "normalAvailable")
        void available_product_is_decremented_without_notification(Product product) throws Exception {
            int availableBefore = product.getAvailable();
            int leadTime = product.getLeadTime();
            Order order = saveOrderWith(product);

            processOrder(order);

            Product result = reload(product);
            assertThat(result.getAvailable()).isEqualTo(availableBefore - 1);
            assertThat(result.getLeadTime()).isEqualTo(leadTime);
            verifyNoInteractions(notificationService);
        }

        @ParameterizedTest
        @MethodSource(PROVIDERS + "normalNotAvailableWithLeadTime")
        void out_of_stock_product_with_lead_time_triggers_delay_notification(Product product) throws Exception {
            int leadTime = product.getLeadTime();
            Order order = saveOrderWith(product);

            processOrder(order);

            Product result = reload(product);
            assertThat(result.getAvailable()).isZero();
            assertThat(result.getLeadTime()).isEqualTo(leadTime);
            verify(notificationService, times(1)).sendDelayNotification(leadTime, product.getName());
            verify(notificationService, never()).sendOutOfStockNotification(anyString());
            verify(notificationService, never()).sendExpirationNotification(anyString(), any());
        }

        @ParameterizedTest
        @MethodSource(PROVIDERS + "normalNotAvailableWithoutLeadTime")
        void out_of_stock_product_without_lead_time_does_nothing(Product product) throws Exception {
            Order order = saveOrderWith(product);

            processOrder(order);

            Product result = reload(product);
            assertThat(result.getAvailable()).isZero();
            assertThat(result.getLeadTime()).isZero();
            verifyNoInteractions(notificationService);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // EXPIRABLE products
    // ---------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("EXPIRABLE products")
    class ExpirableProducts {

        @ParameterizedTest
        @MethodSource(PROVIDERS + "expirableAvailableAndNotExpired")
        void available_and_not_expired_product_is_decremented(Product product) throws Exception {
            int availableBefore = product.getAvailable();
            Order order = saveOrderWith(product);

            processOrder(order);

            assertThat(reload(product).getAvailable()).isEqualTo(availableBefore - 1);
            verifyNoInteractions(notificationService);
        }

        @ParameterizedTest
        @MethodSource(PROVIDERS + "expirableExpiredOrNotAvailable")
        void expired_or_not_available_product_is_marked_out_of_stock_and_expiration_notified(Product product) throws Exception {
            Order order = saveOrderWith(product);

            processOrder(order);

            assertThat(reload(product).getAvailable()).isZero();
            verify(notificationService, times(1)).sendExpirationNotification(product.getName(), product.getExpiryDate());
            verify(notificationService, never()).sendDelayNotification(anyInt(), anyString());
            verify(notificationService, never()).sendOutOfStockNotification(anyString());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // SEASONAL products
    // ---------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("SEASONAL products")
    class SeasonalProducts {

        @ParameterizedTest
        @MethodSource(PROVIDERS + "seasonalInSeasonAndAvailable")
        void in_season_and_available_product_is_decremented(Product product) throws Exception {
            int availableBefore = product.getAvailable();
            Order order = saveOrderWith(product);

            processOrder(order);

            assertThat(reload(product).getAvailable()).isEqualTo(availableBefore - 1);
            verifyNoInteractions(notificationService);
        }

        @ParameterizedTest
        @MethodSource(PROVIDERS + "seasonalNotAvailableOrNotInSeasonAndOutOfSeasonAfterDelay")
        void not_available_or_not_in_season_and_out_of_season_after_delay_is_marked_out_of_stock(Product product) throws Exception {
            Order order = saveOrderWith(product);

            processOrder(order);

            assertThat(reload(product).getAvailable()).isZero();
            verify(notificationService, times(1)).sendOutOfStockNotification(product.getName());
            verify(notificationService, never()).sendDelayNotification(anyInt(), anyString());
            verify(notificationService, never()).sendExpirationNotification(anyString(), any());
        }

        @ParameterizedTest
        @MethodSource(PROVIDERS + "seasonalSeasonNotStartedYet")
        void season_not_started_product_is_notified_out_of_stock_and_stock_is_untouched(Product product) throws Exception {
            int availableBefore = product.getAvailable();
            Order order = saveOrderWith(product);

            processOrder(order);

            assertThat(reload(product).getAvailable()).isEqualTo(availableBefore);
            verify(notificationService, times(1)).sendOutOfStockNotification(product.getName());
            verify(notificationService, never()).sendDelayNotification(anyInt(), anyString());
            verify(notificationService, never()).sendExpirationNotification(anyString(), any());
        }

        @ParameterizedTest
        @MethodSource(PROVIDERS + "seasonalInSeasonNotAvailableRestockBeforeSeasonEnd")
        void in_season_out_of_stock_product_restockable_before_season_end_triggers_delay_notification(Product product) throws Exception {
            int leadTime = product.getLeadTime();
            Order order = saveOrderWith(product);

            processOrder(order);

            Product result = reload(product);
            assertThat(result.getAvailable()).isZero();
            assertThat(result.getLeadTime()).isEqualTo(leadTime);
            verify(notificationService, times(1)).sendDelayNotification(leadTime, product.getName());
            verify(notificationService, never()).sendOutOfStockNotification(anyString());
            verify(notificationService, never()).sendExpirationNotification(anyString(), any());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Mixed order (all product types together) — one dataset per unit-test branch
    // ---------------------------------------------------------------------------------------------

    @Test
    void order_with_every_product_type_processes_each_item_independently() throws Exception {
        LocalDate today = LocalDate.now();
        // NormalProductOrderProcessorTest : not available, leadTime = 1 -> delay notification
        Product normalNotAvailable = new Product(null, 1, 0, ProductType.NORMAL,
                "product2", null, null, null);
        // ExpirabaleProductOrderProcessorTest : several units, expires tomorrow -> decremented
        Product expirableOk = new Product(null, 10, 5, ProductType.EXPIRABLE,
                "prod", today.plusDays(1), null, null);
        // ExpirabaleProductOrderProcessorTest : expired but still has stock -> expiration notification
        Product expirableExpired = new Product(null, 10, 5, ProductType.EXPIRABLE,
                "prod", today.minusMonths(1), null, null);
        // SeasonalProductOrderProcessorTest : in season, available -> decremented
        Product seasonalOk = new Product(null, 4, 4, ProductType.SEASONAL,
                "prod", null, today.minusMonths(1), today.plusMonths(1));
        // SeasonalProductOrderProcessorTest : available, season not started -> out of stock notification, stock untouched
        Product seasonalNotStarted = new Product(null, 4, 4, ProductType.SEASONAL,
                "prod", null, today.plusDays(5), today.plusMonths(1));

        Order order = saveOrderWith(normalNotAvailable, expirableOk, expirableExpired, seasonalOk, seasonalNotStarted);

        processOrder(order);

        assertThat(reload(normalNotAvailable).getAvailable()).isZero();
        assertThat(reload(expirableOk).getAvailable()).isEqualTo(4);
        assertThat(reload(expirableExpired).getAvailable()).isZero();
        assertThat(reload(seasonalOk).getAvailable()).isEqualTo(3);
        assertThat(reload(seasonalNotStarted).getAvailable()).isEqualTo(4);

        verify(notificationService, times(1)).sendDelayNotification(1, "product2");
        verify(notificationService, times(1)).sendExpirationNotification("prod", today.minusMonths(1));
        verify(notificationService, never()).sendExpirationNotification("prod", today.plusDays(1));
        verify(notificationService, times(1)).sendOutOfStockNotification("prod");
    }

    @Test
    void processing_the_same_order_twice_decrements_stock_twice() throws Exception {
        // ExpirabaleProductOrderProcessorTest : several units, expires tomorrow
        Product product = new Product(null, 10, 5, ProductType.EXPIRABLE,
                "prod", LocalDate.now().plusDays(1), null, null);
        Order order = saveOrderWith(product);

        processOrder(order);
        processOrder(order);

        assertThat(reload(product).getAvailable()).isEqualTo(3);
        verifyNoInteractions(notificationService);
    }

    @Test
    void other_orders_sharing_a_product_are_not_affected_by_processing_one_order() throws Exception {
        Product product = new Product(null, 10, 5, ProductType.EXPIRABLE,
                "prod", LocalDate.now().plusDays(1), null, null);
        Order processed = saveOrderWith(product);
        Order untouched = new Order();
        untouched.setItems(new HashSet<>(Set.of(reload(product))));
        untouched = orderRepository.save(untouched);

        processOrder(processed);

        assertThat(reload(product).getAvailable()).isEqualTo(4);
        assertThat(orderRepository.findById(untouched.getId())).isPresent();
    }
}
