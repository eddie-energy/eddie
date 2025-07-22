# REST Outbound Connector

The REST outbound connector is available under `<host>:<eddie.management.server.port>/outbound-connectors/rest`.

| Parameter                                 | Type                   | Default       | Description                                                |
|-------------------------------------------|------------------------|---------------|------------------------------------------------------------|
| outbound-connector.rest.enabled           | `true` or `false`      | `false`       | Enables or disables the REST outbound connector.           |
| outbound-connector.rest.retention-time    | ISO-8601 for durations | `PT48H`       | Sets how long the outbound connector retains the messages. |
| outbound-connector.rest.retention-removal | Spring Cron Syntax     | `0 0 * * * *` | Sets the interval in which messages are deleted.           |

```properties :spring
outbound-connector.rest.enabled=true
outbound-connector.rest.retention-time=PT48H
outbound-connector.rest.retention-removal=0 0 * * * *
```

## Endpoints

```http request
<!--@include: ../../../outbound-connectors/outbound-rest/outbound-rest-requests.http-->
```
