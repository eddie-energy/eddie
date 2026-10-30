// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.readings;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.EnedisDateTime;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record MeterReading(
        @JsonProperty("idPrm") String usagePointId,
        @JsonProperty("etapeMetier") ReadingType meterReadingType,
        @JsonProperty("periode") Period period,
        @JsonProperty("typeValeur") Optional<String> valueType,
        @JsonProperty("modeCalcul") CalculationMode calculationMode,
        @JsonProperty("pas") Optional<Duration> granularity,
        @JsonProperty("grandeur") List<Reading> readings,
        @JsonProperty("contexte") @Nullable List<Object> context
) {
    public enum ReadingType {
        @JsonProperty("BRUT")
        RAW,
        @JsonProperty("BEST")
        CORRECTED
    }

    public enum Direction {
        @JsonProperty("CONS")
        CONSUMPTION,
        @JsonProperty("PROD")
        PRODUCTION
    }

    public enum CalculationMode {
        @JsonProperty("MESURE")
        MEASURED,
        @JsonProperty("DIFF.INDEX")
        DIFF_INDEX,
        @JsonProperty("INTEG.COURBE")
        INTEGER_CURVE,
        @JsonEnumDefaultValue
        EMPTY
    }

    @JsonInclude(NON_NULL)
    public record Reading(@JsonProperty("grandeurMetier") Direction direction,
                          @JsonProperty("grandeurPhysique") String physicalDirection,
                          @JsonProperty("unite") String unit,
                          @JsonProperty("points") List<Point> points,
                          @JsonProperty("calendrier") @Nullable List<Object> calendar) {}

    @JsonInclude(NON_NULL)
    public record Point(@JsonProperty("v") Double value,
                        @JsonProperty("d") @JsonDeserialize(using = EnedisTimestampDeserializer.class) ZonedDateTime timestamp,
                        @JsonProperty("p") Optional<Duration> granularity,
                        @JsonProperty("n") Optional<String> kind,
                        @JsonProperty("iv") Optional<Double> linkyLikelihoodIndex,
                        @JsonProperty("ec") Optional<Double> additionalStatus
    ) {
        static final class EnedisTimestampDeserializer extends ValueDeserializer<ZonedDateTime> {
            @Override
            public @Nullable ZonedDateTime deserialize(
                    JsonParser p,
                    DeserializationContext ctxt
            ) throws JacksonException {
                return new EnedisDateTime(p.getString()).toZonedDateTime();
            }
        }
    }

    public record Period(@JsonProperty("dateDebut") LocalDate start,
                         @JsonProperty("dateFin") LocalDate end) {}
}
