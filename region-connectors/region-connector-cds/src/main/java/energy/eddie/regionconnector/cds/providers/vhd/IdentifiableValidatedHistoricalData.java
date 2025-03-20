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
        public List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner> findByAccountId(String accountId) {
            return serviceContracts.stream()
                                   .filter(contract -> contract.getCdsAccountId().equals(accountId))
                                   .toList();
        }

        public List<ServicePointEndpoint200ResponseAllOfServicePointsInner> findServicePointsByServiceContract(String contractNumber) {
            return servicePoints.stream()
                                .filter(point -> point.getCurrentServicecontracts().contains(contractNumber))
                                .toList();
        }

        public List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner> findMeterDevicesByServicePoint(String cdsServicepointId) {
            return meterDevices.stream()
                               .filter(meter -> meter.getCurrentServicepoints().contains(cdsServicepointId))
                               .toList();
        }

        public List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> findUsageSegmentsByMeter(String meterNumber) {
            return usageSegments.stream()
                                .filter(usageSegment -> usageSegment.getRelatedMeterdevices().contains(meterNumber))
                                .toList();
        }
    }
}
