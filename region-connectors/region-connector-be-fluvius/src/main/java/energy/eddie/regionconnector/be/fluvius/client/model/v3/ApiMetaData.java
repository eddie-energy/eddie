// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public record ApiMetaData(
        @JsonProperty("fetchTime")
        @Nullable
        ZonedDateTime fetchTime
) {}

