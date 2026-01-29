// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contains the data of an {@link energy.eddie.aiida.models.record.AiidaRecord}
 */
public record LatestDataSourceRecordDto(
        @JsonProperty Instant timestamp,
        @JsonProperty String name,
        @JsonProperty AiidaAsset asset,
        @JsonProperty UUID dataSourceId,
        @JsonProperty("values") List<AiidaRecordValueDto> aiidaRecordValues
) {
}