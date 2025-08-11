# Metric Outbound Connector

The metric outbound connector performs live mean and median calculation for each permission request status for all region connectors.
When a new [ConnectionStatusMessage](../../../docs/2-integrating/messages/cim/connection-status-messages.md) received, the duration of the previous state of the [Permission Process Model](../../../docs/2-integrating/integrating.md#permission-process-model) is calculated.
Then, the median duration, accumulative mean and count are updated and persisted in metric.permission_request_metrics, from which the metrics report is generated and periodically sent to the configured endpoint.


| Parameter                            | Type                   | Default | Description                                                            |
|--------------------------------------|------------------------|---------|------------------------------------------------------------------------|
| `outbound-connector.metric.enabled`  | `true` or `false`      | `false` | Enables or disables the Metric outbound connector.                     |
| `outbound-connector.metric.interval` | ISO-8601 for durations | `PT12H` | Interval to periodically retrieve and send the metrics (e.g., PT12H).  |
| `outbound-connector.metric.endpoint` | URI                    |         | URL to which the metrics report is sent (e.g., https://eddie.energy/). |                                                                                          

```properties :spring
outbound-connector.metric.enabled=true
outbound-connector.metric.interval=PT12H
outbound-connector.metric.endpoint=https://eddie.energy/
```