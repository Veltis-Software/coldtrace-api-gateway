package com.acme.coldtrace.gateway;

import static org.assertj.core.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GatewayRoutingTest {
  static HttpServer monitoring = stub("monitoring"),
      identity = stub("identity"),
      asset = stub("asset"),
      maintenance = stub("maintenance"),
      ai = stub("ai"),
      subscription = stub("subscription"),
      alert = stub("alert"),
      report = stub("report");
  @LocalServerPort int port;

  static HttpServer stub(String name) {
    try {
      var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            var text =
                name
                    + "|"
                    + exchange.getRequestURI().getPath()
                    + "|"
                    + exchange.getRequestHeaders().getFirst("Authorization")
                    + "|"
                    + exchange.getRequestHeaders().getFirst("X-Correlation-Id");
            var bytes = text.getBytes(StandardCharsets.UTF_8);
            var origin = exchange.getRequestHeaders().getFirst("Origin");
            if (origin != null) exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) {
              out.write(bytes);
            }
          });
      server.start();
      return server;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add(
        "coldtrace.monitoring.url", () -> "http://localhost:" + monitoring.getAddress().getPort());
    r.add("coldtrace.identity.url", () -> "http://localhost:" + identity.getAddress().getPort());
    r.add("coldtrace.asset.url", () -> "http://localhost:" + asset.getAddress().getPort());
    r.add("coldtrace.maintenance.url", () -> "http://localhost:" + maintenance.getAddress().getPort());
    r.add("coldtrace.ai.url", () -> "http://localhost:" + ai.getAddress().getPort());
    r.add("coldtrace.subscription.url", () -> "http://localhost:" + subscription.getAddress().getPort());
    r.add("coldtrace.alert.url", () -> "http://localhost:" + alert.getAddress().getPort());
    r.add("coldtrace.report.url", () -> "http://localhost:" + report.getAddress().getPort());
  }

  @AfterAll
  static void stop() {
    monitoring.stop(0);
    identity.stop(0);asset.stop(0);maintenance.stop(0);ai.stop(0);subscription.stop(0);
    alert.stop(0);
    report.stop(0);
  }

  String request(String path) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer sample")
                .header("X-Correlation-Id", "route-verification")
                .build(),
            HttpResponse.BodyHandlers.ofString())
        .body();
  }

  @Test
  void specificRoutesReachMonitoringAndPreserveHeaders() throws Exception {
    for (var path :
        new String[] {
          "/api/v1/telemetry/batches", "/api/v1/assets/1/state", "/api/v1/assets/states"
        }) assertThat(request(path)).isEqualTo("monitoring|" + path + "|Bearer sample|route-verification");
  }

  @Test
  void organizationScopedRoutesReachTheirIndependentOwners() throws Exception {
    var cases=java.util.Map.ofEntries(
        java.util.Map.entry("/api/v1/organizations/1/assets","asset"),
        java.util.Map.entry("/api/v1/organizations/1/sensor-readings","monitoring"),
        java.util.Map.entry("/api/v1/session/context","identity"),
        java.util.Map.entry("/api/v1/organization-sign-ups","identity"),
        java.util.Map.entry("/api/v1/organizations/1/users","identity"),
        java.util.Map.entry("/api/v1/organizations/1/maintenance-schedules","maintenance"),
        java.util.Map.entry("/api/v1/organizations/1/technical-service-requests","maintenance"),
        java.util.Map.entry("/api/v1/organizations/1/subscription","subscription"),
        java.util.Map.entry("/api/v1/organizations/1/billing/checkout-sessions","subscription"),
        java.util.Map.entry("/api/v1/organizations/1/dashboard/ai-interpretation","ai"),
        java.util.Map.entry("/api/v1/organizations/1/incidents/2/ai-resolution-plans/3/approvals","alert"),
        java.util.Map.entry("/api/v1/organizations/1/reports/2/ai-summary","report"));
    for(var entry:cases.entrySet())assertThat(request(entry.getKey())).startsWith(entry.getValue()+"|"+entry.getKey());
  }

  @Test
  void internalAndUnknownPathsHaveNoCatchAllDestination() throws Exception {
    for(var path:java.util.List.of("/internal/ai/v1/generations/incident-plan","/not-a-public-api")) {
      var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).build(),HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(404);
    }
  }

  @Test
  void incidentWorkflowAndNotificationsReachAlertService() throws Exception {
    assertThat(request("/api/v1/alerts")).startsWith("alert|");
    assertThat(request("/api/v1/alerts/1")).startsWith("alert|");
    for (var path :
        new String[] {
          "/api/v1/alerts/1/acknowledgement",
          "/api/v1/alerts/stream",
          "/api/v1/incidents/1/corrective-actions",
          "/api/v1/incidents/1/resolution",
          "/api/v1/notifications"
        }) assertThat(request(path)).startsWith("alert|" + path);
    assertThat(request("/api/v1/reports/1/result")).startsWith("report|");
  }

  @Test
  void browserPreflightAllowsBearerAndStreamCursorForConfiguredOrigin() throws Exception {
    var response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/alerts/stream"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .header("Origin", "http://127.0.0.1:4200")
                    .header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", "authorization,last-event-id")
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
        .contains("http://127.0.0.1:4200");
  }

  @Test
  void ownerCorsResponseContainsExactlyOneAllowedOrigin() throws Exception {
    var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/session/context"))
        .header("Origin", "http://127.0.0.1:4200").build(), HttpResponse.BodyHandlers.ofString());
    assertThat(response.headers().allValues("Access-Control-Allow-Origin")).containsExactly("http://127.0.0.1:4200");
  }
}
