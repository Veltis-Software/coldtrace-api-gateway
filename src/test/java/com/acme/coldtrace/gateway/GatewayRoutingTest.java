package com.acme.coldtrace.gateway;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GatewayRoutingTest {
 static HttpServer monitoring=stub("monitoring"),backend=stub("backend"),alert=stub("alert");
 @LocalServerPort int port;
 static HttpServer stub(String name) {
  try {var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/",exchange->{
    var text=name+"|"+exchange.getRequestURI().getPath()+"|"+exchange.getRequestHeaders().getFirst("Authorization")+"|"+exchange.getRequestHeaders().getFirst("X-Correlation-Id");var bytes=text.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}
  });server.start();return server;}catch(Exception e){throw new RuntimeException(e);}
 }
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
  r.add("coldtrace.monitoring.url",()->"http://localhost:"+monitoring.getAddress().getPort());r.add("coldtrace.backend.url",()->"http://localhost:"+backend.getAddress().getPort());
  r.add("coldtrace.alert.url",()->"http://localhost:"+alert.getAddress().getPort());
 }
 @AfterAll static void stop() {monitoring.stop(0);backend.stop(0);alert.stop(0);}
 String request(String path) throws Exception {return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Authorization","Bearer sample").header("X-Correlation-Id","tp1-route").build(),HttpResponse.BodyHandlers.ofString()).body();}
 @Test void specificRoutesReachMonitoringAndPreserveHeaders() throws Exception {
  for(var path:new String[]{"/api/v1/telemetry/batches","/api/v1/assets/1/state","/api/v1/assets/states"}) assertThat(request(path)).isEqualTo("monitoring|"+path+"|Bearer sample|tp1-route");
 }
 @Test void organizationScopedLegacyRoutesStayInBrownfield() throws Exception {
  for(var path:new String[]{"/api/v1/organizations/1/assets","/api/v1/organizations/1/sensor-readings","/api/v1/session/context","/api/v1/incidents/1/resolution-plans"}) assertThat(request(path)).startsWith("backend|"+path);
 }
 @Test void onlyExtractedAlertQueriesReachAlertService() throws Exception {
   assertThat(request("/api/v1/alerts")).startsWith("alert|");
   assertThat(request("/api/v1/alerts/1")).startsWith("alert|");
   assertThat(request("/api/v1/alerts/1/acknowledgement")).startsWith("backend|");
 }
}
