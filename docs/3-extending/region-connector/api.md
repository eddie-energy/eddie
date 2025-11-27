# API

The API documentation is split into two parts.
First, the interfaces and annotations needed to start the region connector and set up the internal structure.
Second, the interfaces needed to send and receive messages through the outbound connectors.
The routing is done via the core.
Internal message exchange is done via reactive streams provided by [Project Reactor](https://projectreactor.io/).

## Internal API

### `@RegionConnector` annotation

The [`@RegionConnector`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/RegionConnector.html) annotation enables EDDIE core to pick up the region connector during classpath scanning.
Without it, the region connector will not be started.
It also configures the name of the region connector, which is used for the [dispatcher servlet](./dispatcher-servlet.md) and determines the path for the [region connector frontend](./frontend.md).
The name should be `<two-letter country-code>-<permission-administrator>`, for example,
`at-eda` for the austrian PA called EDA.

### `PermissionRequest`

The [permission request](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/process/model/PermissionRequest.html) is a central interface in managing permissions.
It provides information regarding the start and end of the validated historical data.
For more detailed information on the permission request regarding the permission process model see [permission requests](../../2-integrating/integrating.md#permission-requests).

Furthermore, a permission request contains [data source information](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/DataSourceInformation.html), which identifies the PA that manages the permission and the MDA that is used to request data from.

#### `MeterReadingPermissionRequest`

The [`MeterReadingPermissionRequest`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/process/model/MeterReadingPermissionRequest.html) extends the permission request interface by adding a method to request the latest meter reading.
This is useful for permission requests that are active in the future, i.e. validated historical data is not yet available. It is possible to save the latest data that was received for a specific permission request, which can be reused for the next data request.

### Database access to Permission Requests

Permission requests have to be loaded from a database, for which there are several interfaces.
For the recommended way to persist permission requests see [internal architecture](./internal-architecture.md).
Some [shared functionality](./shared-functionality.md) might need a specific interface to work.

- [`PermissionRequestRepository`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/process/model/persistence/PermissionRequestRepository.html): request permission requests by ID.
- [`StatusPermissionRequestRepository`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/process/model/persistence/StatusPermissionRequestRepository.html): finds permission requests by status.
- [`StalePermissionRequestRepository`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/process/model/persistence/StalePermissionRequestRepository.html): finds all stale permission requests.
  A stale permission request is one that was sent to the PA, but has never been accepted or rejected by the final customer.
  The EP usually defines an upper time limit, within a permission request can be accepted or rejected.
  If that time limit is exceeded, the permission request is considered stale, and can be marked as timed out.
- [`FullPermissionRequestRepository`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/process/model/persistence/FullPermissionRequestRepository.html): a collection of all interfaces from above.

### `DataNeedCalculationService`

The [data need calculation service](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/data/needs/DataNeedCalculationService.html) is responsible for calculating all the details from a data need (that defines the details of a permission request) and a date, which can be persisted in form of a permission request.
For example, a data need defines a relative time frame for the past three months, but a permission request needs concrete dates to be sent to the PA.
The calculation service takes that data need, and returns both, a result containing the actual start and end date for the permission itself, as well as the start and end date of the data that should be requested.
For an overview of all parameters that can be calculated see the [data need calculation result](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/data/needs/DataNeedCalculationResult.html).

An implementation that can be customized is described [here](./shared-functionality.md#dataneedcalculationserviceimpl).

### `IdentifiablePayload`

Often the same payload has to be converted into different representations of the same data.
To aid in this the [
`IdentifiablePayload`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/IdentifiablePayload.html) can be implemented.
It identifies a PA or MDA response with a specific permission request, so it can be converted to, for example, raw data messages or validated historical data market documents.

## External API

The external API utilizes Project Reactor to emit data.
All interfaces, except `RegionConnector` and `RegionConnectorRetransmissionService`, operate a `Flux` that emits the data.

### `RegionConnector`

The [`RegionConnector` interface](https://architecture.eddie.energy/javadoc/energy/eddie/api/v0/RegionConnector.html) is mandatory for each region connector, and provides base functionality such as the [`getMetadata`](#regionconnectormetadata) and the `terminatePermission` methods.

A region connector must allow the eligible party to terminate a permission request once it is accepted.
This is done by sending a [termination document](../../2-integrating/messages/cim/permission-market-documents.md#termination-documents) to EDDIE, which then routes the ID of the permission request to the region connector.
The region connector is responsible for terminating the permission request with the PA and deleting any credentials that would give the EP access to the data of the final customer.
Furthermore, the region connector has to verify that a permission request with that permission ID actually exists.

#### `RegionConnectorMetadata`

The [`RegionConnectorMetadata` object](https://architecture.eddie.energy/javadoc/energy/eddie/api/v0/RegionConnectorMetadata.html) provides essential information about a region connector, such as:

- countries supported by the region connector
- covered metering points
- earliest start for historical metered data
- latest end for historical metered data
- ID of the region connector
- supported data need types, since some region connector might only support validated historical data, which is needed during development
- supported granularities for historical metered data
- time zone of the region connector

Each region connector has to have one implementation, which can be a singleton, since the information it provides are usually static.

### `RegionConnectorRetransmissionService`

The [`RegionConnectorRetransmissionService`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/retransmission/RegionConnectorRetransmissionService.html) allows the implementation of a retransmission service.
Retransmission can be requested by the EP, in case, some validated historical data is missing from a specific permission request.
This works only in cases where the permission request is still active and data can be requested again from the MDA.
The retransmission service gets permission ID and a timeframe, which declares the start and end date of the needed data.

### `ConnectionStatusMessageProvider`

The [`ConnectionStatusMessageProvider`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/ConnectionStatusMessageProvider.html) interface provides the [connection status message](../../2-integrating/messages/cim/connection-status-messages.md) stream to the outbound connectors.
Furthermore, it is used by the core to propagate status changes of a permission request to the EDDIE button.
This allows immediate feedback for permission requests created by final customers.
For a default implementation see the [connection status message provider implementation](./shared-functionality.md#connectionstatusmessagehandler).

### `RawDataProvider`

The [`RawDataProvider`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/RawDataProvider.html) sends a stream of data received from the MDA to the outbound connectors.
It contains some meta information regarding the permission request and the **unchanged** messages received from the MDA.
This is useful for debugging purposes and allows the EP to access the data in the original format if needed.

### CIM v0.82

The following providers are used to produce CIM v0.82 documents.

#### `PermissionMarketDocumentProvider`

The [`PermissionMarketDocumentProvider`](https://architecture.eddie.energy/javadoc/energy/eddie/api/v0_82/PermissionMarketDocumentProvider.html) is the CIM compliant equivalent of the [ConnectionStatusMessageProvider](#connectionstatusmessageprovider).
It sends the permission request status changes as CIM documents to the outbound connectors.
For a default implementation, see the [permission market document provider implementation](./shared-functionality.md#permissionmarketdocumentmessagehandler).

#### `ValidatedHistoricalDataEnvelopeProvider`

The [`ValidatedHistoricalDataEnvelopeProvider`](https://architecture.eddie.energy/javadoc/energy/eddie/api/v0_82/ValidatedHistoricalDataEnvelopeProvider.html) sends a stream of validated historical data.
When metered data is received from the MDA, it has to be converted to a CIM document, before it is emitted via this provider.

#### `AccountingPointEnvelopeProvider`

The [`AccountingPointEnvelopeProvider`](https://architecture.eddie.energy/javadoc/energy/eddie/api/v0_82/AccountingPointEnvelopeProvider.html) emits accounting point data.
Similar to the [`ValidatedHistoricalDataEnvelopeProvider`](#validatedhistoricaldataenvelopeprovider), it converts accounting point data received from the MDA to CIM compliant documents, and emits them.

### CIM v1.04

#### `ValidatedHistoricalDataMarketDocumentProvider`

The [`ValidatedHistoricalDataMarketDocumentProvider`](https://architecture.eddie.energy/javadoc/energy/eddie/api/v1_04/ValidatedHistoricalDataMarketDocumentProvider.html) is the new version of the [`ValidatedHistoricalDataEnvelopeProvider`](./api.md#validatedhistoricaldataenvelopeprovider).
It should be implemented in parallel to the old version, but its implementation should be the priority.
The old version should still be supported to maintain backwards compatibility.
