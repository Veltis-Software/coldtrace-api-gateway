package com.acme.coldtrace.gateway;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
@ConditionalOnProperty(name = "coldtrace.cloud-run.authentication", havingValue = "true")
public class CloudRunIdentityFilter implements GlobalFilter, Ordered {
  private static final String SERVERLESS_AUTHORIZATION = "X-Serverless-Authorization";
  private final WebClient metadata = WebClient.create("http://metadata.google.internal");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
    var destination = route.getUri();
    String host = destination.getHost();
    if (!"https".equals(destination.getScheme()) || host == null || !host.endsWith(".run.app")) {
      return chain.filter(exchange);
    }

    String audience = "https://" + host;
    return metadata.get()
        .uri(builder -> builder
            .path("/computeMetadata/v1/instance/service-accounts/default/identity")
            .queryParam("audience", audience)
            .build())
        .header("Metadata-Flavor", "Google")
        .retrieve()
        .bodyToMono(String.class)
        .timeout(Duration.ofSeconds(5))
        .flatMap(token -> chain.filter(exchange.mutate().request(request -> request
            .headers(headers -> headers.set(SERVERLESS_AUTHORIZATION, "Bearer " + token.trim())))
            .build()));
  }

  @Override
  public int getOrder() {
    return 0;
  }
}
