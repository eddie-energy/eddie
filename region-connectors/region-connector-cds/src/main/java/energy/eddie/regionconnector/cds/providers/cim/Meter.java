package energy.eddie.regionconnector.cds.providers.cim;

import java.util.List;

public record Meter(String meterNumber, String cdsMeterDeviceId, List<UsageSegment> usageSegments) {
}
