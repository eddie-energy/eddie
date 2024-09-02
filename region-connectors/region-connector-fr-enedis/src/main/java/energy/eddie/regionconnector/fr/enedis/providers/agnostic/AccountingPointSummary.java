package energy.eddie.regionconnector.fr.enedis.providers.agnostic;

import energy.eddie.regionconnector.fr.enedis.dto.address.CustomerAddress;
import energy.eddie.regionconnector.fr.enedis.dto.contact.CustomerContact;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.identity.CustomerIdentity;

public record AccountingPointSummary(
        CustomerContract contract,
        CustomerAddress address,
        CustomerIdentity identity,
        CustomerContact contact
) {
}
