# Kafka Connector

All configuration values that are available for Kafka producers and consumers are supported,
see [Producer Configs](https://kafka.apache.org/28/documentation.html#producerconfigs) and [Consumer Configs](https://kafka.apache.org/28/documentation.html#consumerconfigs).
To include them the prefix `kafka.` has to be used.

The following parameters are of special interest:

| Parameter                         | Type                                           | Default | Description                                                                                                                 |
|-----------------------------------|------------------------------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------|
| outbound-connector.kafka.enabled  | `true` or `false`                              | `false` | Enables or disables the kafka connector.                                                                                    |
| outbound-connector.kafka.format   | `json` or `xml`                                | `json`  | Sets the output and input format for kafka topics. If set to json will only accept and produce json messages, same for xml. |
| outbound-connector.kafka.eddie-id | String                                         |         | Sets the ID of the eddie instance in relation to the kafka broker                                                           |
| kafka.bootstrap.servers           | comma-separated _host:port_ tuples (mandatory) |         | A list of host/port pairs to use for establishing the initial connection to the Kafka cluster.                              |

E.g., if Kafka is installed locally:

```properties :spring
outbound-connector.kafka.enabled=true
outbound-connector.kafka.eddie-id=eddie
kafka.bootstrap.servers=localhost:9094
```

## Additional Configuration

The EDDIE framework can produce large amounts of data in a single record.
Kafka can by default not handle data larger than 1 MB, to be able to handle larger messages the size limit of the EDDIE Kafka producer has to be increased.
This only increases the message size limit that the EDDIE Framework can produce.

```properties :spring
### Increase producer message size to 800 Megabit
kafka.buffer.memory=104857600
kafka.message.max.bytes=104857600
kafka.max.request.size=104857600
```

## Topology

> [!INFO]
> For information regarding the topic naming scheme see [Topic Naming Scheme](../../3-extending/add-outbound-connector.md#general-topic-structure)

The outbound-connector defines a basic topology for interacting with EDDIE.
The following topics are created upon starting this outbound-connector:

- `ep.${outbound-connector.kafka.eddie-id}.agnostic.connection-status-message`:
  Provides status change updates related to a permission request
- `ep.${outbound-connector.kafka.eddie-id}.agnostic.raw-data-message`:
  Provides messages from the region-connectors as is, with additional metadata
- `ep.${outbound-connector.kafka.eddie-id}.cim_0_82.permission-md`: Provides permission market documents
- `ep.${outbound-connector.kafka.eddie-id}.cim_0_82.validated-historical-data-md`:
  Provides validated historical data market documents
- `ep.${outbound-connector.kafka.eddie-id}.cim_0_82.accounting-point-md`:
  Provides accounting point market documents
- `fw.${outbound-connector.kafka.eddie-id}.cim_0_82.termination-md`:
  Allows the eligible party to send [termination documents](../../2-integrating/messages/cim/permission-market-documents.md#termination-documents) to terminate a permission request.
- `fw.${outbound-connector.kafka.eddie-id}.cim_0_91_08.retransmissions`:
  Allows the eligible party to send [redistribution transaction request documents](../../2-integrating/messages/cim/redistribution-transaction-request-documents.md) to request validated historical data again.

## Headers of messages

The following headers are set by EDDIE for outbound messages and can be used for further processing.

- `permission-id`: The permission ID that is related to this message
- `connection-id`:
  The connection ID that is related to this and other messages provided by the eligible upon creation of the permission request
- `data-need-id`: The data need ID related to this message

## Termination of Permission Requests

To terminate permission requests, a permission market document in the configured format has to be sent to the termination topic.
The key should be the ID of the region connector, from which the request originated.
The permission market document for termination is described in [termination documents](../../2-integrating/messages/cim/permission-market-documents.md#termination-documents).

> [!Info]  
> Keep in mind that some kafka clients use newlines as message separator, in that case, minimize the message, or change the message separator!

## Requesting Validated Historical Data Again

The redistribution transaction request documents allow requesting timeframes of validated historical data again, after it has already been requested.
This document is described in [Redistribution transaction request documents](../../2-integrating/messages/cim/redistribution-transaction-request-documents.md).
