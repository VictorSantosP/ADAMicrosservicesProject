package com.example.gatewayMS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayMsApplication.class, args);
	}


	@Value("${gateway.internal.secret}")
	private String internalSecret;

	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("products-route", p -> p
						.path("/api/produtos", "/api/produtos/**")
						.filters(f -> f.addRequestHeader(
								"X-Internal-Secret",
								internalSecret
						))
						.uri("http://localhost:8081"))
				.build();
	}

}
