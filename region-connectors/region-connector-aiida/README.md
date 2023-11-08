# Region Connector for AIIDA

This README will guide you through the process of configuring the region connector for AIIDA, enabling near real-time
data receiving.

## Prerequisites

## Configuration of the Region Connector

| Configuration values | Description |
|----------------------|-------------|
| `key`                | Description |

### .properties file

Example configuration for an `application.properties` file:

```properties
KEY=VALUE
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
KEY=VALUE
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.