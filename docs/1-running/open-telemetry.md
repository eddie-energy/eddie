# Open Telemetry for EDDIE

This README should give a minimum running example on how to connect EDDIE to an [open telemetry](https://opentelemetry.io/) collector and connected services.
The [Grafana OTel LGTM Docker image](https://grafana.com/docs/opentelemetry/docker-lgtm/) is used as a preconfigured OpenTelemetry stack.

[The
`/env/otel` directory](https://github.com/eddie-energy/eddie/tree/main/env/otel/) contains configurations to run the previously mentioned open telemetry services.

> [!NOTE]
> For alert management set the variables in [.env_grafana](https://github.com/eddie-energy/eddie/blob/main/env/otel/.env_grafana).
> It contains some defaults for E-Mail alerting, for other options see [Grafana Alerting](https://grafana.com/docs/grafana/latest/alerting/).

The following command starts the open telemetry services.

```shell
docker compose up -d
```

To connect EDDIE to an OpenTelemetry connector, enable OpenTelemetry via the following properties and set the collector endpoint.
For more information regarding configuring OpenTelemetry for EDDIE see the [Spring Boot Starter](https://opentelemetry.io/docs/zero-code/java/spring-boot-starter/).
Setting `logging.structured.format.console` to `ecs`, `gelf`, or `logstash` enables structured logging in JSON format using the given format, for more information see [Spring Boot Logging](https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured).

```properties :spring
otel.sdk.disabled=false
otel.resource.attributes.deployment.environment=dev
otel.resource.attributes.service.name=EDDIE
otel.resource.attributes.service.namespace=eddie.energy
otel.exporter.otlp.endpoint=http://localhost:4318
otel.exporter.otlp.protocol=http/protobuf
logging.structured.format.console=ecs
```

## Alert Management

Alert management can be done out-of-the-box with Grafana, which allows alerting via E-Mail, Webhooks, Slack, etc.
See [Grafana Alerting](https://grafana.com/docs/grafana/latest/alerting/).