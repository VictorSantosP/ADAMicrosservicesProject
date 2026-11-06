package com.example.gatewayMS.infrastructure.security.configuration;

import com.example.gatewayMS.exceptions.SecurityErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class SecurityConfig {

    private final SecurityErrorHandler securityErrorHandler;

    public SecurityConfig(SecurityErrorHandler securityErrorHandler) {
        this.securityErrorHandler = securityErrorHandler;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {

        return http
                .csrf(csrf -> csrf.disable())

                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'none'; " +
                                                "base-uri 'none'; " +
                                                "frame-ancestors 'none'; " +
                                                "form-action 'none'; " +
                                                "object-src 'none'"
                                )
                        )
                        .hsts(hsts -> hsts
                                .maxAge(Duration.ofDays(365))
                        )
                )
                .authorizeExchange(exchanges -> exchanges

                        .pathMatchers("/auth/**").permitAll()

                        .pathMatchers(HttpMethod.GET,
                                "/api/produtos", "/api/produtos/**")
                        .hasAuthority("SCOPE_products:read")

                        .pathMatchers(HttpMethod.POST,
                                "/api/produtos", "/api/produtos/**")
                        .hasAuthority("SCOPE_products:write")

                        .pathMatchers(HttpMethod.PUT,
                                "/api/produtos", "/api/produtos/**")
                        .hasAuthority("SCOPE_products:write")

                        .pathMatchers(HttpMethod.DELETE,
                                "/api/produtos", "/api/produtos/**")
                        .hasAuthority("SCOPE_products:write")

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(Customizer.withDefaults())
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(
                "dev-only-secret-change-this-key-32bytes-minimum"
                        .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder.withSecretKey(key)
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer("http://localhost:9000")
        );

        return decoder;
    }
}
