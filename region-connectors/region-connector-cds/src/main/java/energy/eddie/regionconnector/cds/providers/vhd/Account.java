package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

record Account(String cdsCustomerNumber, List<Meter> meters) {
    record Meter(String meterNumber, List<UsageSegment> usageSegments) {
    }

    record UsageSegment(ZonedDateTime start, ZonedDateTime end, BigDecimal interval,
                               Map<FormatEnum, List<BigDecimal>> usageSegmentValues) {
    }
}
