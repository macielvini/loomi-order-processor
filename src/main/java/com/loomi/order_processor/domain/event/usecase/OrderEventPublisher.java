package com.loomi.order_processor.domain.event.usecase;

import com.loomi.order_processor.domain.order.entity.OrderCreatedEvent;
import com.loomi.order_processor.domain.order.entity.OrderFailedEvent;
import com.loomi.order_processor.domain.order.entity.OrderPendingApprovalEvent;
import com.loomi.order_processor.domain.order.entity.OrderProcessedEvent;

public interface OrderEventPublisher {

    void sendOrderCreatedEvent(OrderCreatedEvent event);
    
    void sendOrderProcessedEvent(OrderProcessedEvent event);
    
    void sendOrderFailedEvent(OrderFailedEvent event);
    
    void sendOrderPendingApprovalEvent(OrderPendingApprovalEvent event);

}
