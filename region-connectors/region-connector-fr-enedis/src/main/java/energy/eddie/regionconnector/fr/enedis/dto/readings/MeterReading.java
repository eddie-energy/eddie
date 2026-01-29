// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.readings;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.LocalDate;
import java.util.List;

// this makes it possible to unwrap the customer object without an additional wrapper object
@JsonTypeName("meter_reading")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public record MeterReading(
        @JsonProperty("usage_point_id")
        String usagePointId,
        @JsonProperty("start")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate start,
        @JsonProperty("end")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate end,
        @JsonProperty("quality")
        String quality,
        @JsonProperty("reading_type")
        ReadingType readingType,
        @JsonProperty("interval_reading")
        List<IntervalReading> intervalReadings) {
}
