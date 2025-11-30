package com.loomi.order_processor.infra.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.loomi.order_processor.app.service.OrderItemValidatorsByProduct;
import com.loomi.order_processor.domain.order.service.ItemHandler;
import com.loomi.order_processor.domain.order.service.ItemHandlerFor;
import com.loomi.order_processor.domain.product.dto.ProductType;
import com.loomi.order_processor.domain.product.dto.ValidatorMap;
import com.loomi.order_processor.domain.product.service.ProductValidator;
import com.loomi.order_processor.domain.product.service.ValidationStrategyFor;

@Configuration
public class ValidatorConfig {
    
    @Bean
    ValidatorMap productValidators(List<ProductValidator> validators) {

        ValidatorMap map = new ValidatorMap();

        for (ProductValidator validator : validators) {

            ValidationStrategyFor annotation = validator.getClass().getAnnotation(ValidationStrategyFor.class);

            if (annotation == null) {
                throw new IllegalStateException(
                        validator.getClass().getName() + " has no @ValidationStrategyFor annotation");
            }

            for (ProductType type : annotation.value()) {

                map.computeIfAbsent(type, t -> new ArrayList<>())
                   .add(validator);
            }
        }

        return map;
    }

    @Bean
    OrderItemValidatorsByProduct orderItemValidators(List<ItemHandler> validators) {

        Map<ProductType, List<ItemHandler>> map = new EnumMap<>(ProductType.class);

        for (ItemHandler validator : validators) {

            ItemHandlerFor annotation = validator.getClass().getAnnotation(ItemHandlerFor.class);

            if (annotation == null) {
                throw new IllegalStateException(
                        validator.getClass().getName() + " has no @OrderItemValidatorFor annotation");
            }

            if (annotation.global()) {
                for (ProductType type : ProductType.values()) {
                    map.computeIfAbsent(type, t -> new ArrayList<>())
                       .add(validator);
                }
            } else {
                for (ProductType type : annotation.value()) {
                    map.computeIfAbsent(type, t -> new ArrayList<>())
                       .add(validator);
                }
            }
        }

        return new OrderItemValidatorsByProduct(map);
    }

}
