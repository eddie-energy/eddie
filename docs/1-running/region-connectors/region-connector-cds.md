# Region Connector for CDS (Carbon Data Specification)

The region connector for CDS allows the EP to request data from CDS compatible servers.
No registration is required, but can be done anyway to create test CDS servers.
See https://cds.utilityapi.com/en/

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                | Description                         |
|-------------------------------------|-------------------------------------|
| `region-connector.cds.enabled`      | Enables the region connector        |
| `region-connector.cds.redirect-url` | The redirect url for the OAuth flow |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.cds.enabled=true
region-connector.cds.redirect.url=https://your-eddie-instance.com/region-connectors/cds/callback
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters, **except the hyphen** (`-`), with an underscore (`_`)
* Delete all hyphens (`-`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_CDS_ENABLED=true
REGION_CONNECTOR_CDS_REDIRECT_URL=https://your-eddie-instance-com/region-connectors/cds/callback
```