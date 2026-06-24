package com.ordersystem.orderservice.infrastructure.web;

import com.ordersystem.orderservice.domain.exception.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

// Captura excepciones y las convierte al formato de error estándar del sistema.
// @RestControllerAdvice intercepta todas las excepciones que salgan de cualquier controller.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(OrderNotFoundException ex) {
        return errorBody("RESOURCE_NOT_FOUND", ex.getMessage());
    }

    // Captura los errores de @Valid — campos faltantes o inválidos en el request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");
        return errorBody("VALIDATION_ERROR", message);
    }

    // Formato de error estándar definido en los estándares del proyecto
    private Map<String, Object> errorBody(String error, String message) {
        return Map.of(
                "error", error,
                "message", message,
                "timestamp", Instant.now().toString()
        );
    }
}