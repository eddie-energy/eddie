# Region Connector for Slovenia (Moj Elektro)

This README will guide you through the process of configuring a region connector for Slovenia.
The OpenAPI specs for Moj Elektro can be found [here](https://docs.informatika.si/mojelektro/api/).

## Prerequisites

- Get an API token from [Moj Elektro](https://mojelektro.si/login) for testing purposes, not required, but useful if you want to test the region connector with data from the Moj Elektro API.

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                       | Description                                                                       |
|--------------------------------------------|-----------------------------------------------------------------------------------|
| `region-connector.si.moj.elektro.enabled ` | `true` or `false`, defaults to `false`. Enables or disables the region connector. |

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

```properties :spring
region-connector.si.moj.elektro.enabled=true
```
