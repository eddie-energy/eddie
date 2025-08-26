package energy.eddie.outbound.metric.model;

import energy.eddie.outbound.metric.generated.PermissionRequestMetrics;
import energy.eddie.outbound.metric.generated.RegionConnectorMetric;

import java.util.List;

public class ExtendedPermissionRequestMetrics extends PermissionRequestMetrics {
    public ExtendedPermissionRequestMetrics(int count, List<RegionConnectorMetric> regionConnectorMetrics) {
        super();
        this.setCount(count);
        this.setRegionConnectorMetrics(regionConnectorMetrics);
    }
}
