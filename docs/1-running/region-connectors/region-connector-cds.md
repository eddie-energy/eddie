# Region Connector for CDS (Carbon Data Specification)

The region connector for CDS allows the EP to request data from CDS compatible servers.
No registration is required, but can be done anyway to create test CDS servers.
See https://cds.utilityapi.com/en/

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                | Description                                                                                                                                                                                                                                     |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.cds.enabled`      | Enables the region connector                                                                                                                                                                                                                    |
| `region-connector.cds.redirect-url` | The redirect url for the OAuth flow. The redirect url will be used when new CDS servers are registered. *Should not be changed after oauth clients have been created!*                                                                          |
| `region-connector.cds.client-name`  | The name that will be used for OAuth clients when a new CDS server is registered.                                                                                                                                                               |
| `region-connector.cds.par.enabled`  | Either `true` or `false` defaults to `false`, if enabled experimental pushed authorization requests are used to send the authorization request to the CDS server. Currently, PAR is not supported by the sandbox environment.                   |
| `region-connector.cds.retry`        | The interval in which permission requests are sent to the CDS server again, after it failed the first time. Works only if pushed authorization requests are enabled, since those can be resend. Uses Spring Cron syntax. Default is every hour. |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.cds.enabled=true
region-connector.cds.redirect.url=${eddie.public.url}/region-connectors/cds/callback
region-connector.cds.client-name=EDDIE
region-connector.cds.par.enabled=false
region-connector.cds.retry=0 0 * * * *
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters, **except the hyphen** (`-`), with an underscore (`_`)
* Delete all hyphens (`-`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_CDS_ENABLED=true
REGION_CONNECTOR_CDS_REDIRECT_URL=${EDDIE_PUBLIC_URL}/region-connectors/cds/callback
REGION_CONNECTOR_CDS_CLIENT_NAME=EDDIE
REGION_CONNECTOR_CDS_PAR_ENABLED=false
REGION_CONNECTOR_CDS_RETRY=0 0 * * * *
```

## Register new CDS Server

A new CDS server can only be registered by the eligible party.
The region connector provides a REST API to register a new CDS server.
Registering the same CDS server twice will not have any effects and will not create new OAuth credentials.

```http request
### Register a new CDS server
POST http://localhost:8080/region-connectors/cds/register
Content-Type: application/json

{
  "cdsServerUri": "{{cds_server_url}}"
}
```