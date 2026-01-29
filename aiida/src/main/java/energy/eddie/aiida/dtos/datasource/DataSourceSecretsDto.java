// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record DataSourceSecretsDto(
        @JsonProperty UUID dataSourceId,
        @JsonProperty String plaintextPassword
) {}
