# Open Telemetry for EDDIE

This README should give a minimum running example on how to connect EDDIE to an [open telemetry](https://opentelemetry.io/) collector and connected services.
For metrics [Prometheus](https://prometheus.io/) is used, for logs [Loki](https://grafana.com/oss/loki/), and for traces [Jaeger](https://www.jaegertracing.io/).

[The
`/env/otel` directory](https://github.com/eddie-energy/eddie/tree/main/env/otel/) contains configurations to run the previously mentioned open telemetry services.

> [!NOTE]
> For alert management set the variables in [.env_grafana](https://github.com/eddie-energy/eddie/tree/main/env/otel/.env_grafana).
> It contains some defaults for E-Mail alerting, for other options see [Grafana Alerting](https://grafana.com/docs/grafana/latest/alerting/).

The following command starts the open telemetry services.

```shell
docker compose up -d
```

To connect EDDIE to the open telemetry collector, it has to be extended with the [open telemetry java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation).
This can be done via the command line simply by adding this option
`-javaagent:/path/to/opentelemetry-javaagent.jar` when executing the JAR or by building the [docker image](https://github.com/eddie-energy/eddie/tree/main/env/otel/Dockerfile) with the agent.
The following environment variables need to be set for the open telemetry java agent to be able to connect to the open telemetry collector.
Add this to the EDDIE docker container or the environment running EDDIE.

```dotenv
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 # Replace by actual host
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
OTEL_SERVICE_NAME=eddie
```

## Alert Management

Alert management can be done out-of-the-box with Grafana, which allows alerting via E-Mail, Webhooks, Slack, etc.
See [Grafana Alerting](https://grafana.com/docs/grafana/latest/alerting/).