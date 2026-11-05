package com.acme.coldtrace.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.*;

/** Routes each capability to its owner; no historical backend catch-all is retained. */
@Configuration
public class GatewayRoutes {
  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder,
      @Value("${coldtrace.identity.url}")String identity,
      @Value("${coldtrace.asset.url}")String asset,
      @Value("${coldtrace.monitoring.url}")String monitoring,
      @Value("${coldtrace.alert.url}")String alert,
      @Value("${coldtrace.report.url}")String report,
      @Value("${coldtrace.maintenance.url}")String maintenance,
      @Value("${coldtrace.ai.url}")String ai,
      @Value("${coldtrace.subscription.url}")String subscription) {
    var routes=builder.routes()
        .route("alert-stream",r->r.order(0).path("/api/v1/alerts/stream").metadata("response-timeout",-1).uri(alert))
        .route("monitoring",r->r.order(1).path("/api/v1/telemetry/**","/api/v1/assets/*/state","/api/v1/assets/states",
            "/api/v1/organizations/*/sensor-readings/**").uri(monitoring))
        .route("assets",r->r.order(2).path("/api/v1/organizations/*/assets/**","/api/v1/organizations/*/locations/**",
            "/api/v1/organizations/*/gateways/**","/api/v1/organizations/*/iot-devices/**","/api/v1/organizations/*/asset-settings/**").uri(asset))
        .route("maintenance",r->r.order(3).path("/api/v1/organizations/*/maintenance-schedules/**",
            "/api/v1/organizations/*/technical-service-requests/**").uri(maintenance))
        .route("subscription",r->r.order(4).path("/api/v1/subscription-plans/**","/api/v1/billing/**",
            "/api/v1/organizations/*/subscription/**","/api/v1/organizations/*/billing/**").uri(subscription))
        .route("ai-dashboard",r->r.order(5).path("/api/v1/organizations/*/dashboard/**").metadata("response-timeout",40000).uri(ai))
        .route("organization-alerts",r->r.order(6).path("/api/v1/organizations/*/incidents/**",
            "/api/v1/organizations/*/notifications/**").metadata("response-timeout",40000).uri(alert))
        .route("alerts",r->r.order(7).path("/api/v1/alerts/**","/api/v1/incidents/**","/api/v1/notifications/**").uri(alert))
        .route("reports",r->r.order(8).path("/api/v1/reports/**","/api/v1/organizations/*/reports/**")
            .metadata("response-timeout",40000).uri(report))
        .route("identity",r->r.order(9).path("/api/v1/authentication/**","/api/v1/session/**",
            "/api/v1/organization-sign-ups/**","/api/v1/password-reset-requests/**","/api/v1/roles/**",
            "/api/v1/organizations","/api/v1/organizations/*","/api/v1/organizations/*/users/**").uri(identity));
    var destinations=java.util.Map.of("identity",identity,"assets",asset,"monitoring",monitoring,"alerts",alert,
        "reports",report,"maintenance",maintenance,"ai",ai,"subscription",subscription);
    destinations.forEach((name,url)->routes.route(name+"-docs",r->r.order(20)
        .path("/"+name+"/v3/api-docs/**","/"+name+"/swagger-ui/**","/"+name+"/swagger-ui.html")
        .filters(f->f.stripPrefix(1)).uri(url)));
    return routes.build();
  }
}
