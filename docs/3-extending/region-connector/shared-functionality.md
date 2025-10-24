# Shared Functionality

Often region connectors have to implement the same functionality slightly different.
For these cases, there is often a cookie cutter implementation that can be customized to be used instead.
There are examples to create an instance for each of them.

> [!INFO]
> For more concrete examples, please see the usages in the source code of EDDIE

## `CommonInformationModelConfiguration`

Sometimes the PA does not provide a unique ID for the eligible party.
This ID is usually needed to create the CIM documents.
In cases, where it is not available the [`CommonInformationModelConfiguration`](https://architecture.eddie.energy/javadoc/energy/eddie/api/cim/config/CommonInformationModelConfiguration.html) can be used.

```java
@Configuration
public class Config {
  @Bean
  public CommonInformationModelConfiguration cimConfig(
          @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingScheme,
          @Value("${" + ELIGIBLE_PARTY_FALLBACK_ID_KEY + "}") String fallbackId
  ) {
    return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingScheme), fallbackId);
  }
}
```

## `DataNeedCalculationServiceImpl`

The data need calculation can be done by creating the [`DataNeedCalculatioServiceImpl`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/services/data/needs/DataNeedCalculationServiceImpl.html) as a Spring bean.
Each region connector should have one bean of the `DataNeedCalculationService` present.
This implementation can be heavily customized to match the requirements of the region connector.

```java
@Configuration
public class Config {
    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(DataNeedsService dataNeedsService, RegionConnectorMetadata metadata) {
      return new DataNeedCalculationServiceImpl(dataNeedsService, metadata);
    }
}
```

## `ConnectionStatusMessageHandler`

The [`ConnectionStatusMessageHandler`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/event/sourcing/handlers/integration/ConnectionStatusMessageHandler.html) is an implementation of the [`ConnectionStatusMessageProvider`](./api.md#connectionstatusmessageprovider).
It provides connection status messages based on permission events provided by an event bus instance.
It can be started as a Spring bean and does not require further configuration than the required input parameters of the constructor.

```java

@Configuration
public class Config {
  @Bean
  public ConnectionStatusMessageHandler<PermissionRequest> connectionStatusMessageHandler(
          EventBus eventBus,
          PermissionRequestRepository<PermissionRequest> repository
  ) {
    return new ConnectionStatusMessageHandler<>(eventBus,
                                                repository,
                                                PermissionRequest::message); // Assumes that there is a message method available
  }
}
```

## `PermissionMarketDocumentMessageHandler`

The [
`PermissionMarketDocumentMessageHandler`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/event/sourcing/handlers/integration/PermissionMarketDocumentMessageHandler.html) is an implementation of the [
`PermissionMarketDocumentProvider`](./api.md#permissionmarketdocumentprovider).
It is an implementation that utilizes permission events and an event bus to create permission market documents.

```java

@Configuration
public class Config {
  @Bean
  public PermissionMarketDocumentMessageHandler<FluviusPermissionRequest> permissionMarketDocumentMessageHandler(
          EventBus eventBus,
          PermissionRequestRepository<PermissionRequest> permissionRequestRepository,
          DataNeedsService dataNeedsService,
          FooBarConfig config,
          CommonInformationModelConfiguration cimConfig
  ) {
    return new PermissionMarketDocumentMessageHandler<>(
            eventBus,
            permissionRequestRepository,
            dataNeedsService,
            config.eligiblePartyId(),
            cimConfig,
            PermissionRequest::transmissionSchedule, // Assumes that there is a transmissionSchedule method available
            ZoneOffset.UTC
    );
  }
}
```

## `JsonRawDataProvider`

The [
`JsonRawDataProvider`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/agnostic/JsonRawDataProvider.html) provides a shared implementation for raw data.
It requires an implementation of the [
`IdentifiablePayload`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/IdentifiablePayload.html), which is a pair of the permission request and the data that was requested from the MDA or PA.
Should only be used in combination with [
`OnRawDataMessagesEnabled`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/agnostic/OnRawDataMessagesEnabled.html).

```java

@Configuration
public class Config {
  @Bean
  @OnRawDataMessagesEnabled
  public RawDataProvider rawDataProvider(
          ObjectMapper objectMapper,
          Flux<IdentifiablePayload> flux
  ) {
    return new JsonRawDataProvider(
            REGION_CONNECTOR_ID,
            objectMapper,
            identifiableApiResponseFlux
    );
  }
}
```

## `CommonTimeoutService`

The [common timeout service](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/timeout/CommonTimeoutService.html) can be declared as spring bean in combination with the
`@EnableScheduling` annotation.
It checks for old permission requests, and emits a timed out event if any are found.

```java
@Configuration
@EnableScheduling
public class Config {
  @Bean
  public CommonTimeoutService(
          FooPermissionRequestRepository repo,
          Outbox outbox,
          TimeoutConfiguration config
  ) {
    return new CommonTimeoutService(
            repo,
            SimpleEvent::new,
            // Factory to create permission events based on permission ID and a permission process status
            outbox,
            config
    );
  }
}
```

## `FulfillmentService`

The [fullfilment service](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/services/FulfillmentService.html) can be used to fulfill a permission request and provides checks if the permission request was fulfilled.

```java

@Configuration
public class Config {
  @Bean
  public FulfillmentService fulfillmentService(Outbox outbox) {
    return new FulfillmentService(outbox, SimpleEvent::new);
  }
}
```

## `MeterReadingPermissionUpdateAndFulfillmentService`

The [
`MeterReadingPermissionUpdateAndFulfillmentService`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/services/MeterReadingPermissionUpdateAndFulfillmentService.html) updates a permission request with the latest meter reading and checks if it is fulfilled.
Only works for permission requests that implement [the
`MeterReadingPermissionRequest` interface](./api.md#meterreadingpermissionrequest).
It requires [the fulfillment service](#fulfillmentservice).

```java

@Configuration
public class Config {
  @Bean
  public MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService(
          FulfillmentService fulfillmentService,
          Outbox outbox
  ) {
    return new MeterReadingPermissionUpdateAndFulfillmentService(
            fulfillmentService,
            (request, date) -> outbox.commit(new InternalPollingEvent(request.permissionId(), date))
    );
  }
}
```

## `CommonFutureDataService`

The
`CommonFutureDataService` can be used to query [FutureData](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/services/CommonFutureDataService.html).
It uses the provided cron expression and the region connectors timezone to schedule polling intervals. It requires a `PollingService` that implements `CommonPollingService` to poll data and a `PermissionRequestRepository` to find all active permission requests.

```java
@Configuration
public class Config{
  @Bean
  public CommonFutureDataService<FooPermissionRequest> commonFutureDataService(
          PollingService pollingService,
          BarPermissionRequestRepository repository,
          RegionConnectorMetadata metadata,
          TaskSchedular taskSchedular,
          DataNeedCalculationService<DataNeed> calculationService
  ){
    return new CommonFutureDataService<>(
            pollingService,
            repository,
            "0 0 17 * * *",
            metadata,
            taskSchedular,
            calculationService
    );
  }
}
```

## `CommonRetransmissionService`

The [
`CommonRetransmissionService`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/retransmission/CommonRetransmissionService.html) is an implementation of the [
`RegionConnectorRetransmissionService`](https://architecture.eddie.energy/javadoc/energy/eddie/api/agnostic/retransmission/RegionConnectorRetransmissionService.html).
It validates the retransmission request for the most common error cases, such as invalid timeframes or unknown permission ID.
The polling of the validated historical data and emittion to the outbound connectors has to be implemented in the [
`PollingFunction`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/retransmission/PollingFunction.html).
If only the validation is needed and not the call to the polling function, only the [
`RetransmissionValidation`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/retransmission/RetransmissionValidation.html) can be used.

```java
@Configuration
public class Config{
  @Bean
  public RetransmissionValidation retransmissionValidation(RegionConnectorMetadata metadata, DataNeedsService dataNeedsService) {
      return new RetransmissionValidation( metadata, dataNeedsService );
  }
  @Bean
  public CommonRetransmissionService<FooPermissionRequest> retransmissionService(
          BarPermissionRequestRepository repository,
          PollingService pollingService,
          RetransmissionValidation validation
  ){
    return new CommonFutureDataService<>( repository, pollingService, validation);
  }
}
```

## Authorization Callback Template

The [authorization callback template](https://github.com/eddie-energy/eddie/blob/main/region-connectors/shared/src/main/resources/templates/authorization-callback.html) is used for OAUTH callbacks or similar.
When a final customer accepts or rejects a permission request, they might be redirected back to EDDIE via a given callback URI.
To inform them about the state of their permission request the thymeleaf template can be used.
The template takes three attributes:

- `status`: can either be `OK`, `ERROR`, or `DENIED`.
  First, when the permission request was accepted, second when there was an invalid response from the PA, and third if it was denied.
- `dataNeedId`: The data need ID of the permission request if available
- `usagePointIds`: The usage point IDs of the permission request

## CIM Utilities and Helper Classes

There are a few [CIM utilities](https://github.com/eddie-energy/eddie/blob/94e5aa040c0f898f8da2485bb6a8b6c62cc0222b/region-connectors/shared/src/main/java/energy/eddie/regionconnector/shared/cim/v0_82) to help build and interact with CIM documents.
It includes wrappers to build envelopes for CIM documents, helper classes for ESMP date-times, etc.
