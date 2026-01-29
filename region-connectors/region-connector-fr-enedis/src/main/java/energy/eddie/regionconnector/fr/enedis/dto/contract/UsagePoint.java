// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsagePoint(
        @JsonProperty("usage_point_id")
        String id,
        @JsonProperty("usage_point_status")
        String status,
        @JsonProperty("meter_type")
        String meterType
) {
}
