# ColdTrace API Gateway

Spring Cloud Gateway keeps a single entry point during gradual service
extraction. Specific routes take precedence over the brownfield fallback.

| Route | Destination |
|---|---|
| `/api/v1/telemetry/**` | Monitoring |
| `/api/v1/assets/*/state`, `/api/v1/assets/states` | Monitoring |
| `/api/v1/alerts`, `/api/v1/alerts/{id}` | Alert |
| Remaining routes | Existing backend |

Bearer tokens are forwarded to the owning service. Correlation identifiers
are validated or generated and propagated. Unsupported alert operations remain
on the brownfield fallback rather than exposing incomplete new handlers.

## Configuration

`PORT` defaults to 8080; `MONITORING_URL` to http://localhost:8083,
`ALERT_URL` to http://localhost:8084 and `BACKEND_URL` to http://localhost:8090.
Health: `/actuator/health`. Routing tests use HTTP stubs and verify destinations
and header propagation. The gateway is stateless and owns no database.

## Build and test

Requires Java 21 and Maven. Install the shared library from tag
`v0.1.0-sprint1` (`08ab5ee5321e3d7e5e4ec0b3c454a5f5da9bfcc3`) first:

```sh
mvn -B -f ../coldtrace-shared/pom.xml install
mvn -B verify
```

Build the standalone image with the sibling library as a named build context:

```sh
docker build --build-context shared=../coldtrace-shared -t coldtrace-api-gateway:dev .
```

The CI workflow installs the pinned shared source, runs tests, builds the image
and uploads test reports. Local orchestration is in `coldtrace-infrastructure`.
