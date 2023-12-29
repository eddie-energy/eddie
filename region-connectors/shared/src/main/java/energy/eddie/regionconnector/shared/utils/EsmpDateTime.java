package energy.eddie.regionconnector.shared.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;

public class EsmpDateTime {

    public static final DateTimeFormatter ESMP_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
    private final ZonedDateTime dateTime;

    public EsmpDateTime(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Creates an EsmpDateTime
     *
     * @param date   a date string without a time component
     * @param format formatter to parse the date string
     * @param zoneId ZonedId of the date string
     */
    public EsmpDateTime(String date, DateTimeFormatter format, ZoneId zoneId) {
        this.dateTime = LocalDate.parse(date, format).atStartOfDay(zoneId);
    }

    public static EsmpDateTime now() {
        return new EsmpDateTime(ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public String toString() {
        return dateTime.format(ESMP_DATE_TIME_FORMATTER);
    }

    public EsmpDateTime plus(TemporalAmount temporalAmount) {
        return new EsmpDateTime(this.dateTime.plus(temporalAmount));
    }
}
