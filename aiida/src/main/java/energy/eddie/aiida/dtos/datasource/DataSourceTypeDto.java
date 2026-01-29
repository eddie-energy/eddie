// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataSourceTypeDto(
        @JsonProperty("identifier") String identifier,
        @JsonProperty("name") String name) { }