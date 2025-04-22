package energy.eddie.regionconnector.cds.providers.cim;

import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public record UsageSegment(ZonedDateTime start, ZonedDateTime end, BigDecimal interval,
                           Map<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum, List<BigDecimal>> usageSegmentValues) {
}
