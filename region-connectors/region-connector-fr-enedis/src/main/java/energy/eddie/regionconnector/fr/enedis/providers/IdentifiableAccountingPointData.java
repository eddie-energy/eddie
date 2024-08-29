package energy.eddie.regionconnector.fr.enedis.providers;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.dto.address.CustomerAddress;
import energy.eddie.regionconnector.fr.enedis.dto.contact.CustomerContact;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.identity.CustomerIdentity;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.AccountingPointSummary;

public record IdentifiableAccountingPointData(
        FrEnedisPermissionRequest permissionRequest,
        CustomerContract contract,
        CustomerAddress address,
        CustomerIdentity identity,
        CustomerContact contact
) implements IdentifiablePayload<FrEnedisPermissionRequest, AccountingPointSummary> {

    @Override
    public AccountingPointSummary payload() {
        return new AccountingPointSummary(contract, address, identity, contact);
    }
}
