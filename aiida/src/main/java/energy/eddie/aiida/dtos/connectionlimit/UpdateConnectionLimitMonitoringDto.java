// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.connectionlimit;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body to assign the data source monitored for connection limit violations of a permission.
 */
public record UpdateConnectionLimitMonitoringDto(
        @JsonProperty
        @NotNull
        @Schema(description = "ID of the outbound data source to monitor.", example = "51d0a13e-688a-454d-acab-7a6b2951cde2")
        UUID dataSourceId
) {
}
