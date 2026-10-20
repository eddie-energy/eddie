// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MeasureType {
    @JsonProperty("CDC")
    LOAD_CURVE,
    @JsonProperty("IDX")
    INDEX,
    @JsonProperty("PMAX")
    MAX_POWER,
    @JsonProperty("ENERGIE")
    ENERGY,
    @JsonProperty("ITC")
    TECHNICAL_AND_CONTRACTUAL
}
