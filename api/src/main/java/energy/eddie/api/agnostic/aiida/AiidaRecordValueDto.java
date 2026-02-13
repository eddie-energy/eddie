// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiidaRecordValueDto(
        @JsonProperty String rawTag,
        @JsonProperty ObisCode dataTag,
        @JsonProperty String rawValue,
        @JsonProperty String value,
        @JsonProperty UnitOfMeasurement rawUnitOfMeasurement,
        @JsonProperty UnitOfMeasurement unitOfMeasurement
) {
}
