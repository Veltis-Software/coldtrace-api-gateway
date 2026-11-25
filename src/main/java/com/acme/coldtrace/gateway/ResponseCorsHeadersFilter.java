package com.acme.coldtrace.gateway;

import java.util.List;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Applies to Java-defined routes as well as property-defined routes. */
@Component
public class ResponseCorsHeadersFilter implements GlobalFilter,Ordered {
  public int getOrder(){return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER-1;}
  public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain) {
    exchange.getResponse().beforeCommit(()->{
      var headers=exchange.getResponse().getHeaders();
      for(var name:List.of("Access-Control-Allow-Origin","Access-Control-Allow-Credentials")) {
        var values=headers.get(name);
        if(values!=null&&values.size()>1)headers.put(name,List.of(values.getFirst()));
      }
      return Mono.empty();
    });
    return chain.filter(exchange);
  }
}
