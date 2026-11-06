package com.example.gatewayMS.exceptions;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorWebExceptionHandler
        implements ErrorWebExceptionHandler {

    private final JsonMapper objectMapper;

    public GlobalErrorWebExceptionHandler(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode status = ex instanceof ResponseStatusException responseStatus
                ? responseStatus.getStatusCode()
                : HttpStatus.INTERNAL_SERVER_ERROR;

        String message = switch (status.value()) {
            case 400 -> "A requisição enviada é inválida.";
            case 404 -> "O recurso solicitado não foi encontrado.";
            case 405 -> "Método HTTP não permitido.";
            case 429 -> "Limite de requisições excedido.";
            default -> status.is4xxClientError()
                    ? "Não foi possível processar a requisição."
                    : "Ocorreu um erro interno no servidor.";
        };

        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                resolvedStatus != null
                        ? resolvedStatus.getReasonPhrase()
                        : "HTTP Error",
                message,
                exchange.getRequest().getPath().value()
        );

        byte[] bytes;

        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JacksonException serializationError) {
            return Mono.error(serializationError);
        }

        var response = exchange.getResponse();

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}