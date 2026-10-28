// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers.agnostic;

import energy.eddie.regionconnector.fr.enedis.dto.address.UsagePointGeneralData;
import energy.eddie.regionconnector.fr.enedis.dto.situation.ContractualSituation;

import java.util.List;

public record AccountingPointSummary(
        List<ContractualSituation> situations,
        UsagePointGeneralData generalData
) {
}