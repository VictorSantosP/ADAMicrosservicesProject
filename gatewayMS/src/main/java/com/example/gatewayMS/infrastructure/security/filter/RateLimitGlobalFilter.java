package com.example.gatewayMS.infrastructure.security.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitGlobalFilter implements GlobalFilter {

    private static final int REQUEST_LIMIT = 60;
    private static final long WINDOW_MILLIS = 60_000;

    private final ConcurrentHashMap<String, Window> windows =
            new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {

        return exchange.getPrincipal()
                .map(principal -> {

                    if (principal instanceof Authentication auth &&
                            auth.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority()
                                            .equals("ROLE_ANONYMOUS"))) {
                        return "";
                    }

                    return principal.getName();
                })
                .filter(name -> !name.isBlank())
                .defaultIfEmpty(getClientIp(exchange))
                .flatMap(key -> {

                    long now = System.currentTimeMillis();
                    AtomicBoolean allowed = new AtomicBoolean();

                    Window current = windows.compute(key, (k, window) -> {

                        if (window == null ||
                                now - window.startedAt() >= WINDOW_MILLIS) {

                            allowed.set(true);
                            return new Window(now, 1);
                        }

                        if (window.count() < REQUEST_LIMIT) {

                            allowed.set(true);

                            return new Window(
                                    window.startedAt(),
                                    window.count() + 1
                            );
                        }

                        allowed.set(false);
                        return window;
                    });

                    if (allowed.get()) {
                        return chain.filter(exchange);
                    }

                    long remaining = WINDOW_MILLIS -
                            (now - current.startedAt());

                    long retryAfter = Math.max(
                            1,
                            (remaining + 999) / 1000
                    );

                    return tooManyRequests(exchange, retryAfter);
                });
    }

    private String getClientIp(ServerWebExchange exchange) {

        InetSocketAddress address =
                exchange.getRequest().getRemoteAddress();

        if (address == null) {
            return "unknown";
        }

        return address.getAddress() != null
                ? address.getAddress().getHostAddress()
                : address.getHostString();
    }

    private Mono<Void> tooManyRequests(
            ServerWebExchange exchange,
            long retryAfter
    ) {

        var response = exchange.getResponse();

        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(
                "Retry-After",
                String.valueOf(retryAfter)
        );

        String body = """
                {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "Limite de requisicoes excedido. Tente novamente mais tarde."
                }
                """;

        var buffer = response.bufferFactory().wrap(
                body.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        return response.writeWith(Mono.just(buffer));
    }

    private record Window(long startedAt, int count) {
    }
}
