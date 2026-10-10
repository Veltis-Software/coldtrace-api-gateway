package com.acme.coldtrace.gateway;

import com.acme.coldtrace.shared.CorrelationIds;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationGlobalFilter implements GlobalFilter, Ordered {
  public int getOrder() {
    return -1;
  }

  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String supplied = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
    String id = CorrelationIds.sanitize(supplied);
    exchange.getResponse().getHeaders().set("X-Correlation-Id", id);
    return chain.filter(
        exchange
            .mutate()
            .request(
                exchange.getRequest().mutate().headers(h -> h.set("X-Correlation-Id", id)).build())
            .build());
  }
}
