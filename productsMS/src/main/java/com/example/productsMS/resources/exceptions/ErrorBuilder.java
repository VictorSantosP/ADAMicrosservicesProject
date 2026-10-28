package com.example.productsMS.resources.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;

public class ErrorBuilder {

    private ErrorBuilder() {
    }

    public static ApiError build(
            HttpStatus status,
            String message,
            ServerWebExchange exchange
    ) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value()
        );
    }
}