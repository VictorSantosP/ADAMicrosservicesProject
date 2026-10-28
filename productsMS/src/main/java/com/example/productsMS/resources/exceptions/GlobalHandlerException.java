package com.example.productsMS.resources.exceptions;

import com.example.productsMS.resources.exceptions.ApiError;
import com.example.productsMS.resources.exceptions.ErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalHandlerException {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalHandlerException.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception exception,
            ServerWebExchange exchange
    ) {
        logger.error(
                "Erro inesperado ao processar a requisição: {}",
                exchange.getRequest().getPath().value(),
                exception
        );

        ApiError error = ErrorBuilder.build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno ao processar a requisição.",
                exchange
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}