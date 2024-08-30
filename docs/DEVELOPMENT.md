# Development

Philosophy: A developer should be able to clone this repository and be able to build it with one command.

## Links and references

- [Spring Boot: 2. Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config):
  Describes the configuration mechanisms present in spring
  boot. The chapter about type safe configuration properties is especially
  interesting: [2.8. Type-safe Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties).
- [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web)
- [Javalin documentation](https://javalin.io/documentation) and
  [Javalin Javadoc](https://javadoc.io/doc/io.javalin/javalin/latest/index.html) - web framework for example app
- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html) - build tool
    - [Gradle Java Library Plugin](https://docs.gradle.org/current/userguide/java_library_plugin.html)
    - [Gradle Java ApplicationPlugin](https://docs.gradle.org/current/userguide/application_plugin.html)
- [Project Reactor Documentation site](https://projectreactor.io/docs) - reactive library for Java
- [SL4J user manual](https://www.slf4j.org/manual.html) - logging API


- [MDN: Using custom elements](https://developer.mozilla.org/en-US/docs/Web/API/Web_components/Using_custom_elements)
- [Lit Documentation](https://lit.dev/docs/) and [Lit Tutorials](https://lit.dev/tutorials/) - Simple. Fast. Web
  Components.
- [Bootstrap Docs](https://getbootstrap.com/docs/) - CSS Framework

## How to add a new region connector

- Annotate its Spring Configuration class with
    - `@EnableWebMvc`
    - `@SpringBootApplication`
    - `@RegionConnector(name = "foo")`
- it will then be started automatically, provided there is a property *region-connector.__foo__.enabled=true*
- The name that is passed to the annotation determines the path under which the connector element JavaScript file will
  be served, e.g. */region-connectors/__foo__/ce.js*
    - This JS must not be manually served by the RC but is done via a common *region connector processor*
- There are several interface a region connector can implement and thereby make data available. E.g. if the region
  connector implements the `Mvp1ConnectionStatusMessageProvider` interface, it has to provide a stream
  of `ConnectionStatusMessages`. When implementing such an interface from the `api` package, the region connector is
  automatically registered for the correct service by a *region connector processor*, and e.g. the consumption records
  are then streamed via Kafka to the EP.
- A default ControllerAdvice called `RegionConnectorsCommonControllerAdvice`, which handles commonly occurring
  exceptions in a unified way, is automatically registered for all region connectors. Therefore, only exceptions that
  are not handled by this advice need to be explicitly handled in the region connector. If you want to test the error
  responses with your custom controller, you can either add a @TestConfiguration to your test class:
    - ```
      @TestConfiguration
      static class ControllerTestConfiguration {
        @Bean
        public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
          return new RegionConnectorsCommonControllerAdvice();
        }
      }
      ```
    - or add a bean of type `RegionConnectorsCommonControllerAdvice` to your region connector Spring config.
- A region connector should expose a POST endpoint for creating a new permission request and a GET endpoint for fetching
  the current status of a permission request. To ensure consistency between the region connectors, String constants for
  these endpoints are defined in the package `energy.eddie.regionconnector.shared.web.RestApiPaths`.

See the existing region connectors as references.

### Implementing a region connector frontend

The frontend of each region connector is to be implemented as a custom element.
The custom element will be loaded with the following attributes:

- `connection-id`: Optional value that can be used to identify a customer.
- `data-need-id`: Required by the backend to identify the requested data.
- `accounting-point-id`: Optional default for the accounting point ID.
- `jump-off-url`: Optional URL to link or redirect to after the permission request is submitted.
- `company-id`: Optional identifier of the permission administrator.

Elements should extend the `PermissionRequestFormBase` class, which provides helpers for sending the permission request and sending user notifications.

Elements should only use existing components from the [Shoelace](https://shoelace.style/) library or shared custom elements.
There should be no need for custom CSS.

Region connectors will typically include a form for the user to input the necessary data for the permission request.
The envisioned order of elements is:

1. Accounting Point ID (using the name used by the permission administrator)
2. Refresh tokens, API keys, address, or similar
3. Additional and optional fields
4. Submit button
5. Information on how to proceed after submit
6. Additional UI to load after submit

Which fields are present and required may vary between region connectors.

Fields and instructions should be provided in English and use the same terminology as the permission administrator.
Help texts on input fields are encouraged to guide the user in providing the correct information.
