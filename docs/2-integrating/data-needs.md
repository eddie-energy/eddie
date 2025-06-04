# Data Needs

Data needs define the format of permission requests and the data they provide.
Each permission request has to have a data need.
The data need defines what types of data should be requested, a timeframe for data that has a start and end data, etc.
There are multiple types of data needs with some common attributes.
The following diagram shows the different types of data needs and their base type.

```mermaid
<!-- @include: ../../data-needs/data-needs.mermaid -->
```

## Types of Data Needs

The type of data need can describe which data is requested via a permission request and from where the data is requested.
For example, a validated historical data data need, requests data from a MDA, while an AIIDA data need requests data from AIIDA devices.

### AiidaDataNeed

The AiidaDataNeed specifies that a permission request is only for data from an AIIDA device.
It defines what schema should be used to send the data, such as CIM or as raw data.
Furthermore, the asset from which the data is received is defined.

### ValidatedHistoricalDataDataNeed

This data need is used to collect [validated historical data](./messages/cim/validated-historical-data-market-documents.md) from an MDA for a final customer.
It defines what kind of energy type should be collected for and the allowed min and max granularity of the data.
The granularity defines the reading interval or calculated resolution of meter readings.
If an MDA cannot provide the data in the requested min-max granularity interval, the region connector cannot create a permission request for that data need.

### AccountingPointDataNeed

This data need is used to request [accounting point data](./messages/cim/accounting-point-data-market-documents.md) from an MDA for a final customer.
Accounting point data is related to the metering points owned by a final customer.

## Data need configuration

A data need describes a configuration for the _Connect Button_. By using that button, the type of data and time frame is
predefined so that the EP application receives data that it actually needs to perform its job.

Data needs can be configured in two ways: via a JSON file that is read on startup, they can be created via a REST-ful
API which stores the data needs in the core's database.

| Parameter                                | Type              | Description                                            |
|------------------------------------------|-------------------|--------------------------------------------------------|
| eddie.data-needs-config.data-need-source | CONFIG / DATABASE | Specifies the location where data needs are read from. |

If this is set to `CONFIG`, the property `EDDIE_DATA_NEEDS_CONFIG_FILE` needs to be set, otherwise the file is ignored.
It is not possible to combine `CONFIG` and `DATABASE` modes.

### Data needs in config mode

See the file [env/data-needs.json](https://github.com/eddie-energy/eddie/blob/main/env/data-needs.json) for a full example configuration.
While the REST-API ignores the ID field for creation requests, when supplying data needs via the JSON file,
the ID is a mandatory field.

```json
{
  "type": "validated",
  "id": "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
  "name": "LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY",
  "description": "Historical validated consumption data for the last three months, one measurement per day",
  "purpose": "Some purpose",
  "policyLink": "https://example.com/toc",
  "duration": {
    "type": "relativeDuration",
    "start": "-P3M",
    "end": "P0D"
  },
  "energyType": "ELECTRICITY",
  "minGranularity": "P1D",
  "maxGranularity": "P1D",
  "enabled": true,
  "regionConnectorFilter": {
    "type": "blocklist",
    "regionConnectorIds": ["es-datadis"]
  }
}
```

All data needs have these common fields:

| Attribute             | Type    | Description                                                                                                                                                                                                                                         |
|-----------------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| type                  | String  | Type of the data need, e.g. `validated` for historical validated consumption data. Please check the OpenAPI documentation for all supported values.                                                                                                 |
| id                    | String  | Unique id that can be used to reference this data need.                                                                                                                                                                                             |
| name                  | String  | Short memorable name of the data need that may be presented to the customer.                                                                                                                                                                        |
| description           | String  | Multiline string that describes this data need in a human readable form to be shown in the UI.                                                                                                                                                      |
| purpose               | String  | Multiline string that describes the purpose of this data need.                                                                                                                                                                                      |
| policyLink            | URL     | URL to the data policy that applies to this data need.                                                                                                                                                                                              |
| enabled               | boolean | Enables or disables a data need.                                                                                                                                                                                                                    |
| regionConnectorFilter | Object  | _Optional_ If set describes either which region connectors are allowed to process the data need (type: `allowlist`) or which region connectors are blocked from processing the data need (type: `blocklist`). Completely omit it for no restriction |

Depending on the `type`, a data need may require more fields, e.g. for validated historical consumption data:

> [!INFO]
> Please see the OpenAPI documentation (default: [http://localhost:8080/data-needs/swagger-ui/index.html](http://localhost:8080/data-needs/swagger-ui/index.html)) for further details about all possible data need types and their respective fields.

| Attribute      | Type   | Description                                                                                                                                                                                                                                                                                                                                |
|----------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| duration       | Object | Describes the timeframe for this data need.                                                                                                                                                                                                                                                                                                |
| energyType     | String | Type of energy to be requested. See OpenAPI documentation for all possible values.                                                                                                                                                                                                                                                         |
| minGranularity | String | Desired granularity of the data that should be requested.                                                                                                                                                                                                                                                                                  |
| maxGranularity | String | Maximum accepted granularity. Not all MDAs supply the data in the same granularity, if your application can handle multiple granularities, set this to a higher value than `minGranularity` and the region connectors will automatically retry to fetch the data in a higher granularity if the data is not available in `minGranularity`. |

A data need is mandatory for each _Connect with EDDIE_ Button.

### Data needs in database mode

While configuring data needs via JSON files is straightforward, the file cannot be modified while running EDDIE.
The alternative is to configure data needs via the database.
To use the database mode see the [start of this section](#data-need-configuration).
When the database mode is activated, data needs are created via a REST API.
For all data need management APIs see the swagger documentation hosted by EDDIE, default: [http://localhost:8080/data-needs/swagger-ui/index.html?urls.primaryName=Data%20needs%20management%20API](http://localhost:8080/data-needs/swagger-ui/index.html?urls.primaryName=Data%20needs%20management%20API)

> [!WARNING]
> Please note that while the REST-ful API allows that data needs are deleted,
> it might not be a good idea to delete a data need in production.
> This is because permission requests reference the data need and deleting the data need renders the associated permission request useless.
