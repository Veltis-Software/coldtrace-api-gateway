# ColdTrace API Gateway

Spring Cloud Gateway is the single entry point of ColdTrace. It routes each
public path to the microservice that owns it, forwards bearer tokens and
propagates correlation identifiers. In Google Cloud Run every destination is a
private service; the gateway adds a Cloud Run identity token to each call.
The gateway is stateless and owns no database.

## Routes

| Path | Destination |
|---|---|
| `/api/v1/telemetry/**`, `/api/v1/assets/*/state`, `/api/v1/assets/states`, `/api/v1/organizations/*/sensor-readings/**` | Monitoring |
| `/api/v1/organizations/*/assets/**`, `locations`, `gateways`, `iot-devices`, `asset-settings` | Asset |
| `/api/v1/organizations/*/maintenance-schedules/**`, `technical-service-requests` | Maintenance |
| `/api/v1/subscription-plans/**`, `/api/v1/billing/**`, `/api/v1/organizations/*/subscription/**`, `/api/v1/organizations/*/billing/**` | Subscription |
| `/api/v1/organizations/*/dashboard/**` | AI Assistance |
| `/api/v1/alerts/**` (including the `stream` SSE endpoint), `/api/v1/incidents/**`, `/api/v1/notifications/**`, `/api/v1/organizations/*/incidents/**`, `/api/v1/organizations/*/notifications/**` | Alert |
| `/api/v1/reports/**`, `/api/v1/organizations/*/reports/**` | Report |
| `/api/v1/authentication/**`, `/api/v1/session/**`, `/api/v1/organization-sign-ups/**`, `/api/v1/password-reset-requests/**`, `/api/v1/roles/**`, `/api/v1/organizations`, `/api/v1/organizations/*/users/**` | Identity |

Each service's OpenAPI document is published under
`/{service}/v3/api-docs` and `/{service}/swagger-ui.html`, where `{service}` is
`identity`, `assets`, `monitoring`, `alerts`, `reports`, `maintenance`, `ai` or
`subscription`.

## Configuration

| Variable | Default |
|---|---|
| `PORT` | `8080` |
| `IDENTITY_URL` | `http://localhost:8081` |
| `ASSET_URL` | `http://localhost:8082` |
| `MONITORING_URL` | `http://localhost:8083` |
| `ALERT_URL` | `http://localhost:8084` |
| `REPORT_URL` | `http://localhost:8085` |
| `MAINTENANCE_URL` | `http://localhost:8086` |
| `AI_URL` | `http://localhost:8087` |
| `SUBSCRIPTION_URL` | `http://localhost:8088` |

Health: `/actuator/health`.

## Build and test

Requires Java 21 and Maven. Install the shared library from the sibling
`coldtrace-shared` repository first:

```sh
mvn -B -f ../coldtrace-shared/pom.xml install
mvn -B verify
```

`mvn verify` runs the routing tests and the Cucumber acceptance suite in
`src/test/resources/features`. Build the image with the shared library as a
named build context:

```sh
docker build --build-context shared=../coldtrace-shared -t coldtrace-api-gateway:dev .
```

Local orchestration of all services is in `coldtrace-infrastructure`.
