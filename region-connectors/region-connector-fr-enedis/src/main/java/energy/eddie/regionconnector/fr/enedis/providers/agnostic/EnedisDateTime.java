package energy.eddie.regionconnector.fr.enedis.providers.agnostic;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

public class EnedisDateTime {
    public static final DateTimeFormatter ENEDIS_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd")
            .optionalStart()
            .appendPattern(" HH:mm:ss")
            .optionalEnd()
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();
    @Nullable
    private final String dateTime;

    public EnedisDateTime(@Nullable String dateTime) {this.dateTime = dateTime;}

    public @Nullable ZonedDateTime toZonedDateTime() {
        if (dateTime == null) {
            return null;
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, ENEDIS_DATE_TIME_FORMATTER);
            return localDateTime.atZone(ZONE_ID_FR).withZoneSameInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
