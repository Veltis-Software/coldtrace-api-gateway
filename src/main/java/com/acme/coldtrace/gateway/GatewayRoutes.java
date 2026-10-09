package com.acme.coldtrace.gateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.*;
@Configuration
public class GatewayRoutes {
  @Bean public RouteLocator routes(RouteLocatorBuilder builder,
      @Value("${coldtrace.monitoring.url}") String monitoring,
      @Value("${coldtrace.backend.url}") String backend,
      @Value("${coldtrace.alert.url}") String alert) {
    return builder.routes()
      .route("monitoring",r->r.order(0).path("/api/v1/telemetry/**","/api/v1/assets/*/state","/api/v1/assets/states").uri(monitoring))
      .route("monitoring-docs",r->r.order(1).path("/monitoring/v3/api-docs","/monitoring/swagger-ui/**","/monitoring/swagger-ui.html").filters(f->f.stripPrefix(1)).uri(monitoring))
      .route("alert",r->r.order(2).path("/api/v1/alerts","/api/v1/alerts/{id}").uri(alert))
      .route("brownfield",r->r.order(100).path("/**").uri(backend))
      .build();
  }
}
