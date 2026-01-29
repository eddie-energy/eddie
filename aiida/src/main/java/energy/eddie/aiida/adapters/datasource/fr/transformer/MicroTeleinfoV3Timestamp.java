// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.fr.transformer;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MicroTeleinfoV3Timestamp(@JsonProperty String dst, @JsonProperty String date) {}
