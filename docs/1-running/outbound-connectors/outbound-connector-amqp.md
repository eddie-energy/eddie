# Outbound-connector for AMQP

The outbound-connector for AMQP offers functionality to connect EDDIE to an AMQP 1.0 compatible broker.
It has been tested with [RabbitMQ 4.0](https://www.rabbitmq.com/).

## Topology

The outbound-connector defines a basic topology for interacting with EDDIE.
The following exchanges and queues are created upon starting this outbound-connector:

- `status-messages`: Provides status change updates related to a permission request
- `raw-data-in-proprietary-format`: Provides messages from the region-connectors as is, with additional metadata
- `permission-market-documents`: Provides permission market documents
- `validated-historical-data`: Provides validated historical data market documents
- `accounting-point-market-documents`: Provides accounting point market documents
- `terminations`: Allows the eligible party to send termination documents to terminate a permission request.
  For an example of termination document, see [termination messages](../OPERATION.md#termination-of-permission-requests)

## Headers

The following headers are set by EDDIE for outbound messages and can be used for further routing the messages between exchanges and queues.

- `permission-id`: The permission ID that is related to this message
- `connection-id`: The connection ID that is related to this and other messages provided by the eligible upon creation of the permission request
- `data-need-id`: The data need ID that is related to this message

## Configuration

The following configuration values are supported by the AMQP outbound-connector:

| Configuration values              | Possible values                                                                                       | Default | Description                                                 |
|-----------------------------------|-------------------------------------------------------------------------------------------------------|---------|-------------------------------------------------------------|
| `outbound-connector.amqp.enabled` | `true` or `false`                                                                                     | `false` | Enables or disables the outbound-connector.                 |
| `outbound-connector.amqp.format`  | `xml` or `json`                                                                                       | `json`  | Sets the format of the messages that are sent and received. |
| `outbound-connector.amqp.uri`     | [URI](https://docs.oasis-open.org/amqp/addressing/v1.0/csd01/addressing-v1.0-csd01.html#_Toc69731043) |         | Sets the URI of the AMQP 1.0 broker.                        |

### .properties file

Example configuration for an `application.properties` file:

```properties
outbound-connector.amqp.enabled=true
outbound-connector.amqp.format=json
outbound-connector.amqp.uri=amqp://user:password@localhost:5672
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
OUTBOUND_CONNECTOR_AMQP_ENABLED=true
OUTBOUND_CONNECTOR_AMQP_FORMAT=json
OUTBOUND_CONNECTOR_AMQP_URI=amqp://user:password@localhost:5672
```
