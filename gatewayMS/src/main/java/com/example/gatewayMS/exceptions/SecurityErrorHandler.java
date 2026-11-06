package com.example.gatewayMS.exceptions;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class SecurityErrorHandler implements
        ServerAuthenticationEntryPoint,
        ServerAccessDeniedHandler {

    private final JsonMapper objectMapper;

    public SecurityErrorHandler(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> commence(
            ServerWebExchange exchange,
            AuthenticationException exception) {

        return writeError(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Autenticação necessária ou token inválido."
        );
    }

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            AccessDeniedException exception) {

        return writeError(
                exchange,
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Você não tem permissão para acessar este recurso."
        );
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            String error,
            String message) {

        var response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.empty();
        }

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                exchange.getRequest().getPath().value()
        );

        byte[] bytes;

        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JacksonException exception) {
            return Mono.error(exception);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        var buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}