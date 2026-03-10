// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.admin.console.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatusMessageDTO(
        @JsonProperty String permissionId,
        @JsonProperty String regionConnectorId,
        @JsonProperty String dataNeedId,
        @JsonProperty String country,
        @JsonProperty String dso,
        @JsonProperty String startDate,
        @JsonProperty String status,
        @JsonProperty String cimStatus
) {}