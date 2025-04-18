---
prev:
  text: "Add a Region Connector"
  link: "./region-connector/add-region-connector.md"
next:
  text: "Edit Documentation"
  link: "./documentation.md"
---

# Add an outbound-connector

An outbound-connector is a Spring Boot application.
The starting point is a configuration class annotated with:

- `@SpringBootApplication`
- `@OutboundConnector(name = "outbound-connector-name")`

The core module scans the classpath for all classes residing in the `energy.eddie` package and annotated with the `@OutboundConnector`-annotation.
Outbound-connectors are started in their own spring context and dispatcher servlet, which is registered in the parent context (core module).
The outbound-connector will behave like it is its own spring application with a few deviations.
First, all beans defined in the core module are available in the child contexts.
If the core defines a bean, it will be automatically available in the outbound-connector.
For more information, see section [Beans of interest](#beans-of-interest)
Second, each outbound-connector runs in a dispatcher servlet, meaning it is possible to add Spring WebMVC Controllers.
This is described in section [Dispatcher-Servlet](#dispatcher-servlet-web-interface)
Third, outbound-connectors have to be explicitly enabled, otherwise they will not be started by the core module.
More on that in subsubsection [the enabled property](#the-enabled-property).

A short example for a minimal outbound-connector:

```java
import energy.eddie.api.agnostic.outbound.OutboundConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OutboundConnector(name = "example")
public class ExampleOutboundConnector {
}
```

## Interfaces

An outbound-connector can support multiple interfaces.
The interfaces are separated into two packages:

- `energy.eddie.api.agnostic.outbound`: The interfaces in this package are not bound by a version
- `energy.eddie.api.v0_82.outbound`: The current version of the CIM (Common Information Model)

### `@OutboundConnector`

The [`@OutboundConnector`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/agnostic/outbound/OutboundConnector.html)-annotation denotes the starting point of an outbound-connector.
Must be used in combination with the `@SpringBootApplication`-annotation.
The class annotated with the `@OutboundConnector`-annotation will be used to start the Spring context of the outbound-connector.
Furthermore, it is used to set the name of the outbound-connector.

### `ConnectionStatusMessageOutboundConnector`

The [`ConnectionStatusMessageOutboundConnector`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/agnostic/outbound/ConnectionStatusMessageOutboundConnector.html) interface provides means to get a stream of [connection status messages](../2-integrating/messages/connection-status-messages.md), which are emitted to the eligible party.

### `RawDataOutboundConnector`

The [`RawDataOutboundConnector`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/agnostic/outbound/RawDataOutboundConnector.html) interface provides a stream of raw data messages.
[Raw data messages](../2-integrating/messages/raw-data-messages.md) are messages that are received from region-connectors and their region as is, without any changes, which can be useful for debugging purposes or operating on the data provided by metered data administrators itself.

### `PermissionMarketDocumentOutboundConnector`

The [`PermissionMarketDocumentOutboundConnector`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/v0_82/outbound/PermissionMarketDocumentOutboundConnector.html) interface provides a stream of permission market documents.
Their purpose is similar to the connection status messages described in subsubsection [`ConnectionStatusMessageOutboundConnector`](#connectionstatusmessageoutboundconnector).
They provide information about status changes of a certain permission request in a CIM compliant format.
For more information see section [permission market documents](../2-integrating/messages/permission-market-documents.md).

### `ValidatedHistoricalDataMarketDocumentOutboundConnector`

The [`ValidatedHistoricalDataMarketDocumentOutboundConnector`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/v0_82/outbound/ValidatedHistoricalDataEnvelopeOutboundConnector.html) interface provides a stream of validated historical data market documents.
These are CIM compliant documents containing metered data, for more information see section [validated historical data market documents](../2-integrating/messages/validated-historical-data-market-documents.md).

### `AccountingPointEnvelopeOutboundConnector`

The [`AccountingPointEnvelopeOutboundConnector`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/v0_82/outbound/AccountingPointEnvelopeOutboundConnector.html) interface provides a stream of accounting point market documents.
The accounting point market documents are CIM compliant documents, for more information see section [accounting point market documents](../2-integrating/messages/accounting-point-data-market-documents.md).

### `TerminationConnector`

The [`TerminationConnector`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/v0_82/outbound/TerminationConnector.html) interface provides the eligible party with means to change the status of a permission request.
If a permission request has the status accepted, the eligible party can terminate a permission request by sending a termination document, which is a permission market document.
See subsection [termination documents](../2-integrating/messages/permission-market-documents.md#termination-documents) and the [permission process model](../2-integrating/integrating.md#permission-process-model) documentation for more information.
This interface does not produce any documents, but receives them.

### `RetransmissionOutboundConnector`

> [!WARNING]
> This interface is still work in progress.

The [`RetransmissionOutboundConnector`](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/agnostic/outbound/RetransmissionOutboundConnector.html) interface provides the eligible party with means to request retransmission of data from a permission request.
This interface receives retransmission requests and passes them on to the region-connectors.
The interface also informs about the result of the retransmission request in the form of a retransmission results.
These results are only specific to permission requests and no order is guaranteed.
Aka when sending multiple retransmission request for the same permission request there is no way to associate a retransmission result with a specific retransmission request, only with the permission request.

Currently, there are no cim documents defined for retransmission requests.
So the internal `RetransmissionRequest` and
`RetransmissionResult` can be used to send and receive retransmission requests.

## General Topic Structure

To be compatible with different kings of messaging infrastructure (Kafka, NATS, RabbitMQ, Pulsar) the topic names (or
queues, streams, whatever it's named in the respective messaging infrastructure) are made of a prefix and a suffix.
Depending on the actual messaging solution, they are composed differently.

- **Prefix:** eddie.**&lt;direction>**, e.g. `eddie.ep`
- **Suffix:** **&lt;datamodel/version>**.**&lt;topic>**, e.g. `cim_0_82.permission-md`

Applied to the different solutions:

- **Kafka:** Kafka-Topic: **&lt;Prefix>**.**&lt;Suffix>**, e.g. `eddie.ep.cim_0_82.permission-md`
- **AMQP1.0 (out):** Topic: **&lt;Prefix>**.**&lt;Suffix>**, e.g.: `topic://eddie.ep.cim_0_82.permission-md`
- **AMQP1.0 (in):** Queue: **&lt;Prefix>**.**&lt;Suffix>**, e.g.: `queue://eddie.fw.cim_0_82.termination-md`
- **NATS:** Subject: **&lt;Prefix>**.**&lt;Suffix>**, e.g. `eddie.ep.cim_0_82.permission-md`
- **Pulsar:** Tenant: configurable, using "eddie.app" as default; Namespace: **&lt;Prefix>**; Topic **&lt;Suffix>**,
  e.g. `persistent://eddie.app/eddie.ep/cim_0_82.permission-md`

**Topic names are composed of the following fields:**

| Field         | Values             | Description                                                                           |
|---------------|--------------------|---------------------------------------------------------------------------------------|
| EDDIE-id      | eddie              | A constant prefix to group all eddie framework related topics                         |
| direction     | ep / fw            | ep: message from the EDDIE fw to the elibible party app, fw: message for the EDDIE fw |
| datamodel     | cim_0_82 / cim_1_0 | Datamodel combined with version                                                       |
| document type | _see below_        | Type of document exchanged                                                            |

## CIM Topic Structure

**CIM Document types:**

| Document type                 | Direction | Description                                                             |
|-------------------------------|-----------|-------------------------------------------------------------------------|
| termination-md                | fw        | used to terminate a connection / subscription                           |
| permission-md                 | ep        | informs about changes of the current permission status                  |
| accounting-point-md           | ep        | contains accounting point metadata from the MDA                         |
| validated-historical-data-md  | ep        | contains validated historical consumption data (VHCD) from the MDA      |
| redistribution-transaction-rd | ep        | contains VHCD updates to already received VHCD documents _cim_1_0 only_ |

- _-md_: market document
- _-rd_: request document

### CIM 1.0 Kafka

Kafka topics can be configured to allow for better integration into existing Kafka environments. The hash `#` at the
beginning of the property name is a placeholder for the prefix `eddie.outbound.kafka.cim10`.

| Spring Property                              | Default Kafka Topic                                      | Document Structure Link            |
|----------------------------------------------|----------------------------------------------------------|------------------------------------|
| `#.fw.cim_1_0.termination-md`                | `queue://eddie.fw.cim_1_0.termination-md`                | [termination-md](#)                |
| `#.ep.cim_1_0.permission-md`                 | `queue://eddie.ep.cim_1_0.permission-md`                 | [permission-md](#)                 |
| `#.ep.cim_1_0.accounting-point-md`           | `queue://eddie.ep.cim_1_0.accounting-point-md`           | [accounting-point-md](#)           |
| `#.ep.cim_1_0.validated-historical-data-md`  | `queue://eddie.ep.cim_1_0.validated-historical-data-md`  | [validated-historical-data-md](#)  |
| `#.ep.cim_1_0.redistribution-transaction-rd` | `queue://eddie.ep.cim_1_0.redistribution-transaction-rd` | [redistribution-transaction-rd](#) |

### CIM 1.0 AMQP 1.0

AMQP  topics can be configured to allow for better integration into specific AMQP brokers because the address syntax of queues and topics
is broker dependent and not part of the AMQP 1.0 standard. The hash `#` at the beginning of the property name is a placeholder for the
prefix `eddie.outbound.amqp10.cim10`.

| Spring Property                              | Default AMQP 1.0 Address                         | Document Structure Link            |
|----------------------------------------------|--------------------------------------------------|------------------------------------|
| `#.fw.cim_1_0.termination-md`                | `eddie.fw.cim_1_0.termination-md`                | [termination-md](#)                |
| `#.ep.cim_1_0.permission-md`                 | `eddie.ep.cim_1_0.permission-md`                 | [permission-md](#)                 |
| `#.ep.cim_1_0.accounting-point-md`           | `eddie.ep.cim_1_0.accounting-point-md`           | [accounting-point-md](#)           |
| `#.ep.cim_1_0.validated-historical-data-md`  | `eddie.ep.cim_1_0.validated-historical-data-md`  | [validated-historical-data-md](#)  |
| `#.ep.cim_1_0.redistribution-transaction-rd` | `eddie.ep.cim_1_0.redistribution-transaction-rd` | [redistribution-transaction-rd](#) |

## Shared Functionality

A lot of protocols have similar concepts, for example, AMQP's queue and Apache Kafka's topics.
To generalize a few aspects, the [outbound-shared](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/outbound/shared/package-summary.html) module can be used.
It provides classes for consistent naming or serialization and deserialization(SerDe).

### Names

To get the names for endpoints, which can be HTTP endpoints, AMQP Queues, or Kafka topics, the [TopicStructure](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/outbound/shared/TopicStructure.html) class can be used.
For names for headers, such as HTTP headers, Kafka headers, or AMQP properties, the [Headers](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/outbound/shared/Headers.html) class provides constants for that.
Of course, the endpoints and headers are not limited to those values, but it should provide standard names for endpoints/headers which provide the same data.

### Serialization/Deserialization

For common formats, there are already serializers and deserializers in place, which can be reused.

- For `JSON` use the [JsonMessageSerde](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/outbound/shared/serde/JsonMessageSerde.html) class.
- For `XML` use the [JsonMessageSerde](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/outbound/shared/serde/XmlMessageSerde.html) class.
  Supports CIM documents as well as unknown types, which are serialized using the XmlMapper from jackson.

#### Custom SerDe

To implement a custom SerDe for other formats, such as `CSV` or `protobuf`, implement the [MessageSerde](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/outbound/shared/serde/MessageSerde.html) interface and either extend the [DefaultSerdeFactory](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/outbound/shared/serde/DefaultSerdeFactory.html) or implement a custom [SerdeFactory](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/outbound/shared/serde/SerdeFactory.html).

## Security Configuration

Outbound Connectors that should be secured using spring security (like the Admin Console), can define a `SecurityFilterChain` using the [OutboundConnectorSecurityConfig](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/api/agnostic/outbound/OutboundConnectorSecurityConfig.html) Annotation.
The endpoints can then be secured as follows, but please note that the security config should only define rules for the paths of the certain outbound-connector.
```java
@OutboundConnectorSecurityConfig
public class OCSecurityConfig {
    @Bean
    public MvcRequestMatcher.Builder requestMatcher(HandlerMappingIntrospector introspector) {
      return new MvcRequestMatcher.Builder(introspector).servletPath("/" + ALL_OUTBOUND_CONNECTORS_BASE_URL_PATH + "/" + "oc-name");
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {
        return http
                .csrf((csrf) -> csrf.requireCsrfProtectionMatcher(requestMatcher.pattern("*")))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(requestMatcher.pattern("**")).authenticated()
                        .anyRequest().permitAll()
                )
                // ... formLogin, oauth2Login, etc.
                .build();
    }
}
```
> [!IMPORTANT]  
> The filter chains are loaded by eddie core before the context of the outbound-connector is built.
> Consequently, `Beans` that are part of the outbound-connector are ***not*** available in the security config.
> Nevertheless, you can access e.g. `@Values` that are instantiated by eddie core.

## Configuration

Since the outbound-connector is a Spring Boot application, it is possible to load configurations from all sources as usual in Spring Boot.
It is possible to use the `@Value`-annotation to get configuration values, as well as using `@ConfigurationProperties` to configure the outbound-connector.

### Naming conventions

While the names of the configuration properties are arbitrary the naming convention is to use `outbound-connector.<outbound-connector-name>.<property-name>`.
This is useful because it helps differentiate between the configuration of different outbound-connectors.
In some cases this can be ignored, for example, when libraries are used, which read the properties themselves.
In that case, it is often not possible to use the naming convention

### Special configuration properties

There are a few predefined properties, which are either needed by the core module or used by convention.

#### The `enabled` property

**Important**: There is one special configuration property that must be set to `true` to enable the outbound-connector.
Otherwise, the outbound-connector will not be started.

```properties
outbound-connector.<outbound-connector-name>.enabled=true

# Example
outbound-connector.kafka.enabled=true
```

#### The `format` property

The format property is a convention that allows users of the outbound-connector to specify in which format the outbound-connector should send its data.
While some outbound-connectors, like the [admin console](../1-running/admin-console.md) do not use that property, because they are showing the data in a human-readable format, others might support different formats in which data is produced and consumed.
The [kafka outbound-connector](../1-running/outbound-connectors/outbound-connector-kafka.md) supports `json` and `xml`.

Outbound-connectors that send data in a machine-readable format can benefit from defining and using this property, but the core module does not enforce the usage of the format property.
There is no rule on which formats need to be supported, but most [interfaces and their payload](#interfaces) are intended to be serialized to JSON or XML.

```properties
outbound-connector.<outbound-connector-name>.format=format

# Example
outbound-connector.kafka.format=xml
```

## Dispatcher Servlet (web interface)

Each outbound-connector is started in its own spring context and runs in a separate dispatcher servlet.
This means that each outbound-connector can serve data via HTTP to configure it or even use HTTP to send and receive documents.
The outbound-connector is available under `<hostname>:<eddie.management.server.port>/outbound-connectors/<outbound-connector-name>`.

## Beans of interest

The following beans are registered in the parent context and are available in the child contexts.

### DataNeedsService

The `DataNeedsService` allows the outbound-connector to query data needs, described in section [data need configuration](../1-running/OPERATION.md#data-need-configuration).
This allows the outbound-connector to implement complex routing logic based on the data need.
