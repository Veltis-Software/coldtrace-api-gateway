package com.acme.coldtrace.gateway.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GatewayRoutingSteps {
  private static final Map<String, HttpServer> OWNERS = new LinkedHashMap<>();

  static {
    for (String name :
        new String[] {"monitoring", "identity", "asset", "maintenance", "ai", "subscription", "alert", "report"})
      OWNERS.put(name, stub(name));
  }

  @LocalServerPort int port;
  private String path;
  private String correlationId;
  private HttpResponse<String> response;

  @DynamicPropertySource
  static void owners(DynamicPropertyRegistry registry) {
    OWNERS.forEach(
        (name, server) ->
            registry.add("coldtrace." + name + ".url", () -> "http://localhost:" + server.getAddress().getPort()));
  }

  @When("the web application requests {string} with a bearer token and correlation id {string}")
  public void request(String path, String correlationId) throws Exception {
    this.path = path;
    this.correlationId = correlationId;
    response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Authorization", "Bearer sample")
                    .header("X-Correlation-Id", correlationId)
                    .build(),
                HttpResponse.BodyHandlers.ofString());
  }

  @When("an anonymous client requests {string}")
  public void anonymous(String path) throws Exception {
    response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
  }

  @Then("the request is answered by the {string} service with the same path, token and correlation id")
  public void answeredBy(String owner) {
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo(owner + "|" + path + "|Bearer sample|" + correlationId);
  }

  @Then("the gateway answers {int}")
  public void status(int expected) {
    assertThat(response.statusCode()).isEqualTo(expected);
  }

  private static HttpServer stub(String name) {
    try {
      var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            var text =
                name + "|" + exchange.getRequestURI().getPath() + "|"
                    + exchange.getRequestHeaders().getFirst("Authorization") + "|"
                    + exchange.getRequestHeaders().getFirst("X-Correlation-Id");
            var bytes = text.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) {
              out.write(bytes);
            }
          });
      server.start();
      return server;
    } catch (Exception error) {
      throw new IllegalStateException(error);
    }
  }
}
