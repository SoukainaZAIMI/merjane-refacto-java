package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationService notificationService;

    public void notifyDelay(int leadTime, Product product) {
        // Persist the updated lead time before notifying the customer.
        product.setLeadTime(leadTime);
        productRepository.save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }

    public void handleSeasonalProduct(Product product) {
        LocalDate today = LocalDate.now();
        boolean deliveredAfterSeason = arrivesAfterSeason(product, today);

        if (deliveredAfterSeason) {
            // Product is considered unsellable if delivery would happen after season end.
            product.setAvailable(0);
        }

        // Out-of-season paths share the same notification + persistence flow.
        if (deliveredAfterSeason || seasonNotStarted(product, today)) {
            notifyOutOfStockAndSave(product);
            return;
        }

        // In-season products fallback to delay workflow.
        notifyDelay(product.getLeadTime(), product);
    }

    public void handleExpiredProduct(Product product) {
        if (isSellableExpirable(product, LocalDate.now())) {
            // Sell one unit when item is still valid and available.
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
            return;
        }

        // Expired or unavailable items are marked as out of stock and notified.
        notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
        product.setAvailable(0);
        productRepository.save(product);
    }

    private void notifyOutOfStockAndSave(Product product) {
        // Keep notification and persistence together for out-of-season decisions.
        notificationService.sendOutOfStockNotification(product.getName());
        productRepository.save(product);
    }

    private boolean arrivesAfterSeason(Product product, LocalDate today) {
        return today.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate());
    }

    private boolean seasonNotStarted(Product product, LocalDate today) {
        return product.getSeasonStartDate().isAfter(today);
    }

    private boolean isSellableExpirable(Product product, LocalDate today) {
        return product.getAvailable() > 0 && product.getExpiryDate().isAfter(today);
    }
}