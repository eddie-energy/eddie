# Connection status messages

Connection status messages are an EDDIE internal message format and are an alternative version to the permission market documents.
They provide information about the status change of a permission request.
The JSON schema and XSD files can be found [here](https://github.com/eddie-energy/eddie/tree/main/api/src/main/schemas/agnostic).

```mermaid
classDiagram
    class ConnectionStatusMessage {
        +String connectionId
        +UUID permissionId
        +UUID dataNeedId
        +DataSourceInformation dataSourceInformation
        +String timestamp
        +String status
        +String? message
        +Object? additionalInformation
    }

    class DataSourceInformation {
        +String countryCode
        +String meteredDataAdministratorId
        +String permissionAdministratorId
        +String regionConnectorId
    }

    ConnectionStatusMessage "1" --> "1" DataSourceInformation: contains
```

