// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.serializer.LocalDateToEpochSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDate;

public record Cups(
        String cups,
        @JsonSerialize(using = LocalDateToEpochSerializer.class)
        LocalDate startDate,
        @JsonSerialize(using = LocalDateToEpochSerializer.class)
        LocalDate endDate) {
}
