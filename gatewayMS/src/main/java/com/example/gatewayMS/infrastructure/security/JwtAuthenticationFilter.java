package com.example.gatewayMS.infrastructure.security;

import com.example.gatewayMS.infrastructure.security.utils.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain){

        String authHeader =
                exchange.getRequest().getHeaders().getFirst("Authorization");

        if(authHeader ==  null || !authHeader.startsWith("Bearer ")){
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        if(!JwtUtil.validate(token)){
            return unauthorized(exchange);
        }

        return chain.filter(exchange);

    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

}
