# Kafka Connector

All configuration values that are available for Kafka producers and consumers are supported,
see [Producer Configs](https://kafka.apache.org/28/documentation.html#producerconfigs) and [Consumer Configs](https://kafka.apache.org/28/documentation.html#consumerconfigs).
To include them the prefix `kafka.` has to be used.

The following parameters are of special interest:

| Parameter                        | Type                                           | Default        | Description                                                                                                                 |
|----------------------------------|------------------------------------------------|----------------|-----------------------------------------------------------------------------------------------------------------------------|
| outbound-connector.kafka.enabled | `true` or `false`                              | `false`        | Enables or disables the kafka connector.                                                                                    |
| outbound-connector.kafka.format  | `json` or `xml`                                | `json`         | Sets the output and input format for kafka topics. If set to json will only accept and produce json messages, same for xml. |
| kafka.bootstrap.servers          | comma-separated _host:port_ tuples (mandatory) |                | A list of host/port pairs to use for establishing the initial connection to the Kafka cluster.                              |
| kafka.termination.topic          | valid kafka topic name                         | `terminations` | The topic on which the kafka connector listens for termination requests. Optional, the default is `terminations`.           |

E.g., if Kafka is installed locally:

```properties
outbound-connector.kafka.enabled=true
kafka.bootstrap.servers=localhost:9094
```

## Topology

The outbound-connector defines a basic topology for interacting with EDDIE.
The following topics are created upon starting this outbound-connector:

- `status-messages`: Provides status change updates related to a permission request
- `raw-data-in-proprietary-format`: Provides messages from the region-connectors as is, with additional metadata
- `permission-market-documents`: Provides permission market documents
- `validated-historical-data`: Provides validated historical data market documents
- `accounting-point-market-documents`: Provides accounting point market documents
-

`terminations`: Allows the eligible party to send [termination documents](../../2-integrating/messages/permission-market-documents.md#termination-documents) to terminate a permission request.

## Headers of messages

The following headers are set by EDDIE for outbound messages and can be used for further processing.

- `permission-id`: The permission ID that is related to this message
- `connection-id`: The connection ID that is related to this and other messages provided by the eligible upon creation of the permission request
- `data-need-id`: The data need ID related to this message

## Termination of Permission Requests

To terminate permission requests, a permission market document in the configured format has to be sent to the
`kafka.termination.topic`.
The key should be the ID of the region connector, from which the request originated.
The permisison market document for termination is described in [termination documents](../../2-integrating/messages/permission-market-documents.md#termination-documents).

> [!Info]  
> Keep in mind that some kafka clients use newlines as message separator, in that case, minimize the message, or change the message separator!
