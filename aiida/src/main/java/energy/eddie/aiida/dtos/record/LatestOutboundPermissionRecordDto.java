// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record LatestOutboundPermissionRecordDto(
        @JsonProperty UUID permissionId,
        @JsonProperty String topic,
        @JsonProperty String serverUri,
        @JsonProperty List<LatestSchemaRecordDto> messages
) {
}
