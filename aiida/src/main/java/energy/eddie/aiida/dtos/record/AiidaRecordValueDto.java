// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiidaRecordValueDto(
        @JsonProperty String rawTag,
        @JsonProperty ObisCode obisCode,
        @JsonProperty String rawValue,
        @JsonProperty UnitOfMeasurement rawUnit,
        @JsonProperty String value,
        @JsonProperty UnitOfMeasurement unit
) {}