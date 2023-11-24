package energy.eddie.regionconnector.shared.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class EsmpDateTimeUtils {
    public static final DateTimeFormatter ESMP_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

    private EsmpDateTimeUtils() {
    }

    /**
     * Creates a ESMPDateTimeString from a ZonedDateTime.
     * A ESMPDateTimeString is a string in the format yyyy-MM-dd'T'HH:mm'Z' with the timezone set to UTC.
     *
     * @param zonedDateTime ZonedDateTime to convert to ESMPDateTimeString
     * @return The string representation of the ZonedDateTime in the format yyyy-MM-dd'T'HH:mm'Z' with the timezone set to UTC.
     */
    public static String zonedDateTimeToESMPDateTimeString(ZonedDateTime zonedDateTime) {
        return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).format(ESMP_DATE_TIME_FORMATTER);
    }
}