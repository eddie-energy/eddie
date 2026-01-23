// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;

import java.util.UUID;

public record PermissionDetailsDto(
        @JsonProperty(value = "eddie_id")
        UUID eddieId,
        @JsonProperty(value = "permission_request")
        AiidaPermissionRequestInterface request,
        @JsonProperty(value = "data_need")
        DataNeed dataNeed
) {}
