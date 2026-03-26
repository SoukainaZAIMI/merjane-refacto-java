package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@UnitTest
public class OrderProcessingServiceUnitTests {

    @Mock
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderProcessingService orderProcessingService;

    // NORMAL
    @Test
    public void shouldDecrementNormalProductWhenAvailable() {

        Long orderId = 1L;
        Product product = new Product(null, 15, 3, "NORMAL", "USB Cable", null, null, null);

        Long processedOrderId = processOrder(orderId, product);


        assertEquals(orderId, processedOrderId);
        assertSavedWithAvailable(product, 2);
        verify(orderRepository).findById(orderId);
        verifyNoInteractions(productService);
    }

    @Test
    public void shouldNotifyDelayForNormalProductWhenUnavailable() {

        Long orderId = 2L;
        Product product = new Product(null, 7, 0, "NORMAL", "RJ45 Cable", null, null, null);

        processOrder(orderId, product);


        assertDelegatedAndSaved(() -> verify(productService).notifyDelay(7, product), product);
    }

    // SEASONAL
    @Test
    public void shouldDecrementSeasonalProductWhenInSeasonAndAvailable() {

        Long orderId = 3L;
        Product product = new Product(null, 10, 5, "SEASONAL", "Watermelon", null,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10));

        processOrder(orderId, product);


        assertSavedWithAvailable(product, 4);
        verify(productService, never()).handleSeasonalProduct(product);
    }

    @Test
    public void shouldDelegateSeasonalProductWhenOutOfSeason() {

        Long orderId = 4L;
        Product product = new Product(null, 10, 5, "SEASONAL", "Grapes", null,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(30));

        processOrder(orderId, product);


        assertDelegatedAndSaved(() -> verify(productService).handleSeasonalProduct(product), product);
    }

    // EXPIRABLE
    @Test
    public void shouldDecrementExpirableProductWhenNotExpiredAndAvailable() {

        Long orderId = 5L;
        Product product = new Product(null, 0, 8, "EXPIRABLE", "Butter", LocalDate.now().plusDays(3), null, null);

        processOrder(orderId, product);


        assertSavedWithAvailable(product, 7);
        verify(productService, never()).handleExpiredProduct(product);
    }

    @Test
    public void shouldDelegateExpirableProductWhenExpired() {

        Long orderId = 6L;
        Product product = new Product(null, 0, 8, "EXPIRABLE", "Milk", LocalDate.now().minusDays(1), null, null);

        processOrder(orderId, product);


        assertDelegatedAndSaved(() -> verify(productService).handleExpiredProduct(product), product);
    }

    private Long processOrder(Long orderId, Product product) {
        stubOrder(orderId, product);
        return orderProcessingService.processOrder(orderId);
    }

    private void assertSavedWithAvailable(Product product, int expectedAvailable) {
        assertEquals(expectedAvailable, product.getAvailable());
        verify(productRepository, times(1)).save(product);
    }

    private void assertDelegatedAndSaved(Runnable delegatedVerification, Product product) {
        delegatedVerification.run();
        verify(productRepository, times(1)).save(product);
    }

    private void stubOrder(Long orderId, Product product) {
        // Single-product order keeps each test focused on one business rule.
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(createOrder(orderId, product)));
    }

    private Order createOrder(Long id, Product product) {
        Order order = new Order();
        order.setId(id);
        order.setItems(Set.of(product));
        return order;
    }
}
