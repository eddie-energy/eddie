// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.address;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * Envelope for the données générales response, so the connector can read it whether Enedis sends an
 * array or a bare object.
 */
public record UsagePointGeneralDatas(
        @JsonDeserialize(using = UsagePointGeneralDataListDeserializer.class) List<UsagePointGeneralData> generalData
) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public UsagePointGeneralDatas {
        // For JSON Creator
    }
}