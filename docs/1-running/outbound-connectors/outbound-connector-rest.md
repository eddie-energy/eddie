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

The endpoints are documented via OpenAPI and are made available during the runtime under
`<host>:<eddie.management.server.port>/outbound-connectors/rest/v3/api-docs`.
They can be seen via the hosted swagger UI under `<host>:<server.port>/data-needs/swagger-ui/index.html`.
While OpenAPI provides examples and schemas, it is recommended to use [the client library](../../2-integrating/messages/cim/client-libraries.md) to parse CIM documents, since the OpenAPI generated schemas are not true to the actual schema.

```http request
<!--@include: ../../../outbound-connectors/outbound-rest/outbound-rest-requests.http-->
```
