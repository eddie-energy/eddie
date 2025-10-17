# Beans of interest

The following beans are of special interest, because they are either:

- made available to region connectors automatically via the parent context and can be used by the region connectors, for example the [DataNeedsService](#dataneedsservice)
- same as above, but the beans are meant to add functionality, without actually being used by the region connector code itself, for example the [RegionConnectorsCommonControllerAdvice](#regionconnectorscommoncontrolleradvice)
- Spring beans that can enhance EDDIE as a whole.

## `DataNeedsService`

The [
`DataNeedsService`](https://architecture.eddie.energy/javadoc/energy/eddie/dataneeds/services/DataNeedsService.html) is essential for a region connector.
It provides the means to request certain data needs, which can be used to request specific data for a permission request.
For example, if a permission request is tied to an accouting point data need via the data need ID, the region connector can request the data need, check its type, and send the correct request to the MDA.
All this functionality is provided via the `DataNeedsService`.
The
`DataNeedsService` is made available to all region connectors and only needs to be injected into the spring component that requires it.

> [!INFO]
> IntelliJ might give a false positive warning
`SpringJavaInjectionPointsAutowiringInspection`, when injecting beans from parent contexts.

## `TimeoutConfiguration`

The [
`TimeoutConfiguration`](https://architecture.eddie.energy/javadoc/energy/eddie/regionconnector/shared/timeout/TimeoutConfiguration.html) is provided by the core and contains the time limit for stale permission requests.
It can be used to check for stale permission requests.

## `RegionConnectorsCommonControllerAdvice`

A default ControllerAdvice called
`RegionConnectorsCommonControllerAdvice` handles commonly occurring exceptions in a unified way.
It is automatically registered for all region connectors.
Therefore, only exceptions that are not handled by this advice need to be explicitly handled in the region connector.
If you want to test the error responses with your custom controller, you can either add a `@TestConfiguration` to your test class:

```java
      @TestConfiguration
      static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
          return new RegionConnectorsCommonControllerAdvice();
        }
      }
```

or add a bean of type `RegionConnectorsCommonControllerAdvice` to your region connector Spring config.

## Health Indicators

EDDIE requires each region connector to implement a health indicator.
[Health indicators](https://docs.spring.io/spring-boot/api/rest/actuator/health.html) are part of Spring Actuator.
They are automatically picked up by the core.

If a new health indicator is added to a region connector, it has to be added to the health indicator group of the region connector.
This is done by adding the following configuration to the [`application.properties`](https://github.com/eddie-energy/eddie/blob/main/core/src/main/resources/application.properties).
Here is an example:

```properties
management.endpoint.health.group.region-connector-<region-connector-name>.include=<health-indicator-name>
```

This is necessary in order for the admin console to be able to show the health of a specific region connector in the admin console.
