# Configuration

This section gives insights on which files have to be adapted in order to show the new region connector, as well as describing the naming conventions of configuration properties.

## Configuration of Permission Administrators

The EDDIE button will make a request for all available permission administrators to the backend.
In order, to pick up the new region connector, it has to be registered in the [permission-administrator.json](https://github.com/eddie-energy/eddie/blob/main/european-masterdata/src/main/resources/permission-administrators.json)-file.
This contains a configuration for the country, company of the PA, name of the PA, a jump off url, which is the final customer portal of the PA to accept permission requests, and the ID of the region connector.

## Configuration of Metered Data Administrators

Similar to [the configuration of permission administrators](#configuration-of-permission-administrators), the MDA has to be configured to for new region connectors.
They're added to the [metered-data-administrators.json](https://github.com/eddie-energy/eddie/blob/main/european-masterdata/src/main/resources/permission-administrators.json)
Each entry consists of:

- `country`: country of MDA
- `company`: company of MDA
- `companyId`: company ID of MDA
- `websiteUrl`: website URL of MDA
- `officialContact`: official contact for MDA
- `permissionAdministrator`: permission administrator responsible for MDA. can be the same as the MDA.

## Configuration Properties

The configuration names of a region connector follow the naming convention `region-connectors.<two-letter country-code>.<PA name>.<property-name>`.

### The `enabled` property

The `region-connectors.<two-letter country-code>.<PA name>.enabled` property exists for each region connector.
It enables or disables the region connector and defaults to `false`.
