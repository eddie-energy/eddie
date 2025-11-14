# Outbound-connector for AMQP

The outbound-connector for AMQP offers functionality to connect EDDIE to an AMQP 1.0 compatible broker.
It has been tested with [RabbitMQ 4.0](https://www.rabbitmq.com/).

## Topology

> [!INFO]
> For information regarding the topic naming scheme see [Topic Naming Scheme](../../3-extending/add-outbound-connector.md#general-topic-structure)

The outbound-connector defines a basic topology for interacting with EDDIE.
The following topics are created upon starting this outbound-connector:

- `ep.${outbound-connector.amqp.eddie-id}.agnostic.connection-status-message`:
  Provides status change updates related to a permission request
- `ep.${outbound-connector.amqp.eddie-id}.agnostic.raw-data-message`:
  Provides messages from the region connectors as is, with additional metadata
- `ep.${outbound-connector.amqp.eddie-id}.cim_0_82.permission-md`: Provides permission market documents
- `ep.${outbound-connector.amqp.eddie-id}.cim_0_82.validated-historical-data-md`:
  Provides validated historical data market documents
- `ep.${outbound-connector.amqp.eddie-id}.cim_1_04.near-real.time-data-md`:
  Provides near real-time data market documents
- `ep.${outbound-connector.amqp.eddie-id}.cim_0_82.accounting-point-md`:
  Provides accounting point market documents
- `fw.${outbound-connector.amqp.eddie-id}.cim_0_82.termination-md`:
  Allows the eligible party to send [termination documents](../../2-integrating/messages/cim/permission-market-documents.md#termination-documents) to terminate a permission request.
- `fw.${outbound-connector.amqp.eddie-id}.cim_0_91_08.retransmissions`:
  Allows the eligible party to send [redistribution transaction request documents](../../2-integrating/messages/cim/redistribution-transaction-request-documents.md) to request validated historical data again.

## Properties of messages

The following properties are set by EDDIE for outbound messages and can be used for further routing the messages between exchanges and queues.

- `permission-id`: The permission ID that is related to this message
- `connection-id`:
  The connection ID that is related to this and other messages provided by the eligible upon creation of the permission request
- `data-need-id`: The data need ID related to this message

## Configuration

The following configuration values are supported by the AMQP outbound-connector:

| Configuration values               | Type                                                                                                  | Default | Description                                                       |
|------------------------------------|-------------------------------------------------------------------------------------------------------|---------|-------------------------------------------------------------------|
| `outbound-connector.amqp.enabled`  | `true` or `false`                                                                                     | `false` | Enables or disables the outbound-connector.                       |
| `outbound-connector.amqp.format`   | `xml` or `json`                                                                                       | `json`  | Sets the format of the messages that are sent and received.       |
| `outbound-connector.amqp.uri`      | [URI](https://docs.oasis-open.org/amqp/addressing/v1.0/csd01/addressing-v1.0-csd01.html#_Toc69731043) |         | Sets the URI of the AMQP 1.0 broker.                              |
| `outbound-connector.amqp.eddie-id` | String                                                                                                |         | Sets the ID of the eddie instance in relation to the amqp broker. |

### .properties file

Example configuration for an `application.properties` file:

```properties :spring
outbound-connector.amqp.enabled=true
outbound-connector.amqp.format=json
outbound-connector.amqp.uri=amqp://user:password@localhost:5672
outbound-connector.amqp.eddie-id=eddie
```
