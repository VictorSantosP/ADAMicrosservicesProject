
package com.example.productsMS.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class GatewayInternalAuthenticationFilter extends OncePerRequestFilter {

    private final String expectedSecret;

    public GatewayInternalAuthenticationFilter(String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String receivedSecret = request.getHeader("X-Internal-Secret");

        if (receivedSecret == null ||
                !MessageDigest.isEqual(
                        expectedSecret.getBytes(StandardCharsets.UTF_8),
                        receivedSecret.getBytes(StandardCharsets.UTF_8))) {

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "gatewayMS",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_GATEWAY"))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }
}
