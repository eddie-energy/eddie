package energy.eddie.regionconnector.cds.providers;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;

import java.util.List;

public record IdentifiableUsageSegments(CdsPermissionRequest permissionRequest,
                                        List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> payload) implements IdentifiablePayload<CdsPermissionRequest, List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner>> {
}
