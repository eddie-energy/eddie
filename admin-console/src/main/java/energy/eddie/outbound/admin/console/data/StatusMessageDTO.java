// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.admin.console.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public record StatusMessageDTO(@JsonProperty String permissionId,
                               @JsonProperty String regionConnectorId,
                               @JsonProperty String dataNeedId,
                               @JsonProperty String country,
                               @JsonProperty String dso,
                               @JsonProperty String startDate,
                               @JsonProperty String status,
                               @JsonProperty String cimStatus) {

    public ZonedDateTime getParsedStartDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        assert this.startDate != null;
        return ZonedDateTime.parse(this.startDate, formatter);
    }
}