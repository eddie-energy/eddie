# Region Connector for Estonia (Elering)

This README will guide you through the process of configuring a region connector for Estonia.

## Prerequisites

- Register as an energy service provider with [Elering - Estfeed datahub](https://estfeed.elering.ee/) to create an admin account.
- An [estonian e-Residency](https://eresident.politsei.ee/) might be needed to log into their portal and manage the account.
- A technical user can then be created as described in [estfeed-datahub-docs - Users management](https://github.com/Elering/estfeed-datahub-docs/blob/main/eng/03.02-users-management.md) to use the [APIs](https://github.com/Elering/estfeed-datahub-docs/blob/main/eng/03.02-users-management.md). A (test) metering point is required for some of the APIs.

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                   | Description                                                                       |
|----------------------------------------|-----------------------------------------------------------------------------------|
| `region-connector.ee.elering.enabled ` | `true` or `false`, defaults to `false`. Enables or disables the region connector. |

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

```properties :spring
region-connector.ee.elering.enabled=true
```