package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;

import java.util.List;

public record IdentifiableValidatedHistoricalData(CdsPermissionRequest permissionRequest,
                                                  Payload payload
) implements IdentifiablePayload<CdsPermissionRequest, IdentifiableValidatedHistoricalData.Payload> {

    public record Payload(
            List<AccountsEndpoint200ResponseAllOfAccountsInner> accounts,
            List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner> serviceContracts,
            List<ServicePointEndpoint200ResponseAllOfServicePointsInner> servicePoints,
            List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner> meterDevices,
            List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> usageSegments
    ) {
    }
}
