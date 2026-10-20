// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * Envelope for the situation contractuelle response, so the connector can read it as a list while
 * tolerating a bare object payload.
 */
public record ContractualSituations(
        @JsonDeserialize(using = ContractualSituationListDeserializer.class) List<ContractualSituation> situations
) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ContractualSituations {
        // For JSON Creator
    }
}