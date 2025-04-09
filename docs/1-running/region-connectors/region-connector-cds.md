# Region Connector for CDS (Carbon Data Specification)

The region connector for CDS allows the EP to request data from CDS compatible servers.
No registration is required, but can be done anyway to create test CDS servers.
See https://cds.utilityapi.com/en/

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                | Description                                                                                                                                                                                                                                                                                                                                                     |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.cds.enabled`      | Enables the region connector                                                                                                                                                                                                                                                                                                                                    |
| `region-connector.cds.redirect-url` | The redirect url for the OAuth flow. The redirect url will be used when new CDS servers are registered. *Should not be changed after oauth clients have been created!*                                                                                                                                                                                          |
| `region-connector.cds.client-name`  | The name that will be used for OAuth clients when a new CDS server is registered.                                                                                                                                                                                                                                                                               |
| `region-connector.cds.par.enabled`  | Either `true` or `false` defaults to `false`, if enabled experimental pushed authorization requests are used to send the authorization request to the CDS server. Currently, PAR is not supported by the sandbox environment.                                                                                                                                   |
| `region-connector.cds.retry`        | The interval in which messages are sent to the CDS server again, after it failed the first time. There are two types of messages: Creating the permission request at the PA's side, which only works if `region-connector.cds.par.enabled=true`, and terminating permission requests, which works all the time. Uses Spring Cron syntax. Default is every hour. |

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

```properties :spring
region-connector.cds.enabled=true
region-connector.cds.redirect.url=${eddie.public.url}/region-connectors/cds/callback
region-connector.cds.client-name=EDDIE
region-connector.cds.par.enabled=false
region-connector.cds.retry=0 0 * * * *
```

## Register new CDS Server

The region connector provides a REST API to register a new CDS server.
A new CDS server can only be registered by the eligible party.
Therefore, the endpoint is only available via the management port.
Registering the same CDS server twice will not have any effects and will not create new OAuth credentials.

```http request
### Register a new CDS server
POST http://localhost:${eddie.management.server.port}/region-connectors/cds/${eddie.management.server.urlprefix}/register
Content-Type: application/json

{
  "cdsServerUri": "{{cds_server_url}}"
}
```