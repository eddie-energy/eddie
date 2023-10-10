# Region Connector for Spain (Datadis)

This README will guide you through the process of configuring a region connector for Spain.

## Prerequisites

- Register a user with Datadis [here](https://datadis.es/register).

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                   | Description                                                              |
|----------------------------------------|--------------------------------------------------------------------------|
| `region-connector.es.datadis.username` | Username (DNI/NIF) used to log into the Datadis website private area.    |
| `region-connector.es.datadis.password` | Password for the user used to log into the Datadis website private area. |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.es.datadis.username=12345756X
region-connector.es.datadis.password=secret
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_ES_DATADIS_USERNAME=12345756X
REGION_CONNECTOR_ES_DATADIS_PASSWORD=secret
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.