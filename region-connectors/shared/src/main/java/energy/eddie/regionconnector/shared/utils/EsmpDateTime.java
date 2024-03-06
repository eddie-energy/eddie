package energy.eddie.regionconnector.shared.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

/**
 * Implementation of the ESMP DateTime.
 * See <a href="https://eepublicdownloads.entsoe.eu/clean-documents/pre2015/resources/Transparency/MoP%20Ref16%20-%20The%20problem%20statement%20process.pdf#page=11">entsoe.eu</a>
 */
public class EsmpDateTime {

    public static final DateTimeFormatter ESMP_DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
    public static final DateTimeFormatter ESMP_DATE_TIME_SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final ZonedDateTime dateTime;
    private final TemporalUnit precision;

    public EsmpDateTime(ZonedDateTime dateTime) {
        this(dateTime, ChronoUnit.SECONDS);
    }

    /**
     * Creates an EsmpDateTime
     *
     * @param date      a date string without a time component
     * @param format    formatter to parse the date string
     * @param zoneId    ZonedId of the date string
     * @param precision if the formatted string should include second or minute precision
     */
    EsmpDateTime(String date, DateTimeFormatter format, ZoneId zoneId, TemporalUnit precision) {
        this(LocalDate.parse(date, format).atStartOfDay(zoneId), precision);
    }

    EsmpDateTime(ZonedDateTime dateTime, TemporalUnit precision) {
        this.dateTime = dateTime;
        this.precision = precision;
    }

    public static EsmpDateTime now() {
        return now(Clock.systemUTC());
    }

    public static EsmpDateTime now(Clock clock) {
        return new EsmpDateTime(ZonedDateTime.now(clock));
    }


    @Override
    public String toString() {
        ZonedDateTime normalized = dateTime.withZoneSameInstant(ZoneOffset.UTC);
        return precision == ChronoUnit.SECONDS
                ? normalized.format(ESMP_DATE_TIME_SECOND_FORMATTER)
                : normalized.format(ESMP_DATE_TIME_MINUTE_FORMATTER);

    }

    public EsmpDateTime plus(TemporalAmount temporalAmount) {
        return new EsmpDateTime(this.dateTime.plus(temporalAmount));
    }
}
