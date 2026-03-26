package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
public class OrderProcessingService {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    public Long processOrder(Long orderId) {
        // Orchestrates product processing for a single order and returns its id.
        Order order = orderRepository.findById(orderId).get();
        order.getItems().forEach(this::processProduct);
        return order.getId();
    }

    private void processProduct(Product product) {
        // Dispatches processing rules based on the product type.
        switch (product.getType()) {
            case "NORMAL" -> handleNormalProduct(product);
            case "SEASONAL" -> handleSeasonalProduct(product);
            case "EXPIRABLE" -> handleExpirableProduct(product);
            default -> {
                // No-op: unknown product types are ignored.
            }
        }
    }

    private void handleNormalProduct(Product product) {
        if (hasStock(product)) {
            decrementAndSave(product);
            return;
        }

        // If out of stock but lead time exists, trigger a delay workflow.
        if (product.getLeadTime() > 0) {
            productService.notifyDelay(product.getLeadTime(), product);
        }
    }

    private void handleSeasonalProduct(Product product) {
        LocalDate today = LocalDate.now();
        if (hasStock(product) && isInSeason(product, today)) {
            decrementAndSave(product);
            return;
        }

        // Delegate off-season handling to product business rules.
        productService.handleSeasonalProduct(product);
    }

    private void handleExpirableProduct(Product product) {
        if (hasStock(product) && isNotExpired(product, LocalDate.now())) {
            decrementAndSave(product);
            return;
        }

        // Delegate expired/unavailable handling to product business rules.
        productService.handleExpiredProduct(product);
    }

    private boolean hasStock(Product product) {
        return product.getAvailable() > 0;
    }

    private boolean isInSeason(Product product, LocalDate today) {
        return today.isAfter(product.getSeasonStartDate()) && today.isBefore(product.getSeasonEndDate());
    }

    private boolean isNotExpired(Product product, LocalDate today) {
        return product.getExpiryDate().isAfter(today);
    }

    private void decrementAndSave(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
