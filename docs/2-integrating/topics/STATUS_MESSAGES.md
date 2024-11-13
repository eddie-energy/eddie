# Topic: Status Messages

The topic **status-messages** provides status messages for a certain permission. The permission status represents the
current state of the permission with details.
A list of available options for the attribute `status` can be found in
the [permission states](../PERMISSION_STATES.md) documentation.

The schema for status messages looks as follows:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "connectionId": {
      "type": "string",
      "description": "ID of the connection (a connectionId can be associated with multiple permissions)"
    },
    "permissionId": {
      "type": "string",
      "description": "Unique ID of the permission"
    },
    "dataNeedId": {
      "type": "string",
      "description": "ID of the data need associated with the permission"
    },
    "dataSourceInformation": {
      "type": "object",
      "description": "Information about the datasource",
      "properties": {
        "countryCode": {
          "type": "string",
          "description": "The country code of the data source."
        },
        "meteredDataAdministratorId": {
          "type": "string",
          "description": "The metered data administrator id of the data source."
        },
        "permissionAdministratorId": {
          "type": "string",
          "description": "The permission administrator id of the data source."
        },
        "regionConnectorId": {
          "type": "string",
          "description": "The region connector id of the data source."
        }
      },
      "required": [
        "countryCode",
        "meteredDataAdministratorId",
        "permissionAdministratorId",
        "regionConnectorId"
      ]
    },
    "timestamp": {
      "type": "number",
      "format": "double",
      "description": "Timestamp of the message"
    },
    "status": {
      "type": "string",
      "description": "Status of the message"
    },
    "message": {
      "type": "string",
      "description": "Contains additional information about the status"
    }
  },
  "required": [
    "connectionId",
    "permissionId",
    "dataNeedId",
    "dataSourceInformation",
    "timestamp",
    "status"
  ]
}
```