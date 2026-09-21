# Management APIs

To help protect management endpoints without assuming a specific mechanism to implement that protection the EDDIE Framework differentiates between a public port (8080) and management port (9090).

APIs and applications under the management port include:
- [Admin Console](admin-console.md)
- [REST Outbound Connector](outbound-connectors/outbound-connector-rest.md)
- [Data Needs Management](../2-integrating/data-needs.md#data-needs-in-database-mode)
- Region Connector Management (like adding [CDS](region-connectors/region-connector-cds.md#register-new-cds-server) servers)

Ports and paths can be configured using environment variables as described [here](OPERATION.md#configuring-eddie-core).

To provide an example for protecting the management port, a proxy could map one path to the public port and the other to the management port where the management port might require the visitor to enter a username and password .

This setup can be enabled in the compose setup via the `auth` profile.

```shell
# inside the env directory
docker compose up -d --profile auth
```

The profile adds a Caddy instance that directs all traffic to `/eddie` to the EDDIE instance and requires basic authentication with a username and password (`eddie`/`password`) to access management APIs like the admin console.

- http://localhost:9000/eddie/demo is public
- http://localhost:9000/eddie/management/outbound-connectors/admin-console is protected