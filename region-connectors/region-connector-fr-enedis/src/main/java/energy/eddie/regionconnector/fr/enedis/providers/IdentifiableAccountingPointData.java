// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.dto.address.UsagePointGeneralData;
import energy.eddie.regionconnector.fr.enedis.dto.situation.ContractualSituation;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.AccountingPointSummary;

import java.util.List;

public record IdentifiableAccountingPointData(
        FrEnedisPermissionRequest permissionRequest,
        List<ContractualSituation> situations,
        UsagePointGeneralData generalData
) implements IdentifiablePayload<FrEnedisPermissionRequest, AccountingPointSummary> {

    @Override
    public AccountingPointSummary payload() {
        return new AccountingPointSummary(situations, generalData);
    }
}