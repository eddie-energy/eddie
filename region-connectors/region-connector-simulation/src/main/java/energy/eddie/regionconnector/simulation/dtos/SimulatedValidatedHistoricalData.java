// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public record SimulatedValidatedHistoricalData(
        String meteringPoint,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        @JsonProperty("startDateTime")
        Optional<ZonedDateTime> startDateTime,
        Duration meteringInterval,
        List<Measurement> measurements
) {
    @JsonCreator
    public SimulatedValidatedHistoricalData {}

    public SimulatedValidatedHistoricalData(
            String meteringPoint,
            ZonedDateTime startDateTime,
            Duration meteringInterval,
            List<Measurement> measurements
    ) {
        this(meteringPoint, Optional.of(startDateTime), meteringInterval, measurements);
    }

    public Optional<ZonedDateTime> end() {
        return this.startDateTime()
                   .map(start -> start.plus(this.readingDuration()));
    }

    public Duration readingDuration() {
        return this.meteringInterval().multipliedBy(this.measurements().size());
    }
}
