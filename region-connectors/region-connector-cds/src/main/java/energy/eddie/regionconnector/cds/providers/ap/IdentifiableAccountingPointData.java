package energy.eddie.regionconnector.cds.providers.ap;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.cds.openapi.model.AccountsEndpoint200ResponseAllOfAccountsInner;
import energy.eddie.regionconnector.cds.openapi.model.MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner;
import energy.eddie.regionconnector.cds.openapi.model.ServiceContractEndpoint200ResponseAllOfServiceContractsInner;
import energy.eddie.regionconnector.cds.openapi.model.ServicePointEndpoint200ResponseAllOfServicePointsInner;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;

import java.util.List;

public record IdentifiableAccountingPointData(CdsPermissionRequest permissionRequest,
                                              IdentifiableAccountingPointData.Payload payload) implements IdentifiablePayload<CdsPermissionRequest, IdentifiableAccountingPointData.Payload> {
    public record Payload(List<AccountsEndpoint200ResponseAllOfAccountsInner> accounts,
                          List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner> serviceContracts,
                          List<ServicePointEndpoint200ResponseAllOfServicePointsInner> servicePoints,
                          List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner> meterDevices
    ) {}
}
