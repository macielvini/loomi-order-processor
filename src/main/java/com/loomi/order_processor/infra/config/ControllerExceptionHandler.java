package com.loomi.order_processor.infra.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.loomi.order_processor.domain.exception.HttpException;
import com.loomi.order_processor.infra.web.dto.InvalidPropertyDto;

import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class ControllerExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<?> handleHttpExceptions(HttpException e) {
        return ResponseEntity.status(e.status().value()).body(e.toJson());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<InvalidPropertyDto>> handleValidationErrors(MethodArgumentNotValidException e) {
        List<InvalidPropertyDto> dto = new ArrayList<>();

        e.getBindingResult().getFieldErrors().forEach(err -> {
            String message = messageSource.getMessage(err, LocaleContextHolder.getLocale());

            InvalidPropertyDto error = new InvalidPropertyDto(message, err.getField());
            dto.add(error);
        });

        return new ResponseEntity<>(dto, HttpStatus.BAD_REQUEST);
    }

}
