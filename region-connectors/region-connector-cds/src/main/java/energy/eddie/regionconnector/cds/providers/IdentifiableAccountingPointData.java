package energy.eddie.regionconnector.cds.providers;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;

import java.util.List;

public record IdentifiableAccountingPointData(CdsPermissionRequest permissionRequest,
                                              IdentifiableAccountingPointData.Payload payload) implements IdentifiablePayload<CdsPermissionRequest, IdentifiableAccountingPointData.Payload> {
    public record Payload(List<AccountsEndpoint200ResponseAllOfAccountsInner> accounts,
                          List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner> serviceContracts,
                          List<ServicePointEndpoint200ResponseAllOfServicePointsInner> servicePoints,
                          List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner> meterDevices,
                          List<BillSectionEndpoint200ResponseAllOfBillSectionsInner> billSections) {}
}
