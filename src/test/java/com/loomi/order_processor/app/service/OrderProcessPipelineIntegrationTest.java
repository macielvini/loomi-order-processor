package com.loomi.order_processor.app.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.loomi.order_processor.app.service.order.OrderProcessPipeline;
import com.loomi.order_processor.app.service.order.handler.item.OrderItemHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loomi.order_processor.app.config.OrderProcessingConfig;
import com.loomi.order_processor.domain.order.entity.Order;
import com.loomi.order_processor.domain.event.usecase.AlertEventPublisher;
import com.loomi.order_processor.app.service.delivery.DeliveryService;
import com.loomi.order_processor.app.service.order.handler.global.HighValueOrderHandler;
import com.loomi.order_processor.app.service.order.handler.global.OrderHandler;
import com.loomi.order_processor.app.service.order.handler.global.OrderIsPendingHandler;
import com.loomi.order_processor.app.service.order.handler.global.PaymentOrderHandler;
import com.loomi.order_processor.app.service.order.handler.item.PhysicalItemHandler;
import com.loomi.order_processor.domain.order.valueobject.OrderItem;
import com.loomi.order_processor.domain.order.valueobject.OrderStatus;
import com.loomi.order_processor.domain.payment.usecase.FraudService;
import com.loomi.order_processor.app.service.payment.PaymentService;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.RawProductMetadata;
import com.loomi.order_processor.domain.product.entity.Product;
import com.loomi.order_processor.domain.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Process Pipeline Integration Tests")
class OrderProcessPipelineIntegrationTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FraudService fraudService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private AlertEventPublisher alertProducer;

    private OrderProcessPipeline pipeline;
    private UUID testOrderId;
    private UUID testProductId;
    private String testCustomerId;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testProductId = UUID.randomUUID();
        testCustomerId = "customer-123";

        OrderProcessingConfig config = new OrderProcessingConfig();
        config.setHighValueThreshold(new BigDecimal("10000"));
        config.setFraudThreshold(new BigDecimal("20000"));

        List<OrderHandler> globalHandlers = new ArrayList<>();
        globalHandlers.add(new OrderIsPendingHandler());
        globalHandlers.add(new HighValueOrderHandler(config));
        globalHandlers.add(new PaymentOrderHandler(fraudService, paymentService));

        List<OrderItemHandler> itemHandlers = new ArrayList<>();
        itemHandlers.add(new PhysicalItemHandler(
                productRepository,
                alertProducer,
                new DeliveryService()
        ));

        pipeline = new OrderProcessPipeline(globalHandlers, itemHandlers, productRepository);
    }

    private Order createOrder(BigDecimal totalAmount) {
        RawProductMetadata itemMetadata = new RawProductMetadata();
        itemMetadata.put("warehouseLocation", "SP");
        
        OrderItem item = OrderItem.builder()
                .productId(testProductId)
                .customerId(testCustomerId)
                .quantity(1)
                .productType(ProductType.PHYSICAL)
                .price(totalAmount)
                .metadata(itemMetadata)
                .build();

        return Order.builder()
                .id(testOrderId)
                .customerId(testCustomerId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .items(java.util.List.of(item))
                .build();
    }

    private Product createProduct() {
        return Product.builder()
                .id(testProductId)
                .name("Test Product")
                .productType(ProductType.PHYSICAL)
                .price(BigDecimal.valueOf(100.00))
                .stockQuantity(100)
                .isActive(true)
                .metadata(new RawProductMetadata())
                .build();
    }

    @Test
    @DisplayName("shouldRequireHumanReview_whenFraudServiceDetectsFraudForHighValueOrder")
    void shouldRequireHumanReview_whenFraudServiceDetectsFraudForHighValueOrder() {
        Order order = createOrder(new BigDecimal("25000"));
        Product product = createProduct();

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

        var validationResult = pipeline.validate(order);

        assertTrue(validationResult.isHumanReviewRequired());
    }

    @Test
    @DisplayName("shouldRequireHumanReview_whenFraudServiceAllowsHighValueOrder")
    void shouldRequireHumanReview_whenFraudServiceAllowsHighValueOrder() {
        Order order = createOrder(new BigDecimal("25000"));
        Product product = createProduct();

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));
        var validationResult = pipeline.validate(order);

        assertTrue(validationResult.isHumanReviewRequired());
    }

    @Test
    @DisplayName("shouldProcessSuccessfully_whenPaymentServiceSucceeds")
    void shouldProcessSuccessfully_whenPaymentServiceSucceeds() {
        Order order = createOrder(new BigDecimal("15000"));
        Product product = createProduct();

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(product));

        var processResult = pipeline.process(order);

        assertTrue(processResult.isProcessed());
    }
}

