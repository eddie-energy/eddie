# Metric Outbound Connector

The metric outbound connector performs live mean and median calculation for each permission request status for all region connectors.
When a new [ConnectionStatusMessage](../../../docs/2-integrating/messages/cim/connection-status-messages.md) received, the duration of the previous state of the [Permission Process Model](../../../docs/2-integrating/integrating.md#permission-process-model) is calculated.
Then, the median duration, accumulative mean and count are updated and persisted in metric.permission_request_metrics, from which the metrics report is generated and periodically sent to the configured endpoint.


| Parameter                            | Type               | Default                                | Description                                             |
|--------------------------------------|--------------------|----------------------------------------|---------------------------------------------------------|
| `outbound-connector.metric.enabled`  | `true` or `false`  | `false`                                | Enables or disables the Metric outbound connector.      |
| `outbound-connector.metric.eddie-id` | String             |                                        | Identifier of the EDDIE instance.                       |
| `outbound-connector.metric.interval` | Spring Cron Syntax | `0 0 */12 * * *` (i.e. every 12 hours) | Interval to periodically retrieve and send the metrics. |
| `outbound-connector.metric.endpoint` | URI                | https://eddie.energy/metadata-sharing  | URL to which the metrics report is sent.                |                                                                                          

```properties :spring
outbound-connector.metric.enabled=true
outbound-connector.metric.eddie-id=eddie
outbound-connector.metric.interval=0 0 */12 * * *
outbound-connector.metric.endpoint=https://eddie.energy/metadata-sharing
```