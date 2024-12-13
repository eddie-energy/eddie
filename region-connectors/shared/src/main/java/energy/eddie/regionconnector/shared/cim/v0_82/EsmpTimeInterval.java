package energy.eddie.regionconnector.shared.cim.v0_82;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.util.Strings;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Implementation of the ESMP Time Interval.
 * See <a href="https://eepublicdownloads.entsoe.eu/clean-documents/pre2015/resources/Transparency/MoP%20Ref16%20-%20The%20problem%20statement%20process.pdf#page=11">entsoe.eu</a>
 */
public class EsmpTimeInterval {
    @Nullable
    private final EsmpDateTime start;
    @Nullable
    private final EsmpDateTime end;

    public EsmpTimeInterval(@Nullable ZonedDateTime start, @Nullable ZonedDateTime end) {
        this.start = start == null ? null : new EsmpDateTime(start, ChronoUnit.MINUTES);
        this.end = end == null ? null : new EsmpDateTime(end, ChronoUnit.MINUTES);
    }

    public EsmpTimeInterval(@Nullable LocalDate start, @Nullable LocalDate end, ZoneId zoneId) {
        this.start = start == null ? null : new EsmpDateTime(start.atStartOfDay(zoneId), ChronoUnit.MINUTES);
        this.end = end == null ? null : new EsmpDateTime(end.atStartOfDay(zoneId), ChronoUnit.MINUTES);
    }

    public EsmpTimeInterval(String start, String end, DateTimeFormatter format, ZoneId zoneId) {
        this.start = new EsmpDateTime(start, format, zoneId, ChronoUnit.MINUTES);
        this.end = new EsmpDateTime(end, format, zoneId, ChronoUnit.MINUTES);
    }

    @Nullable
    public String start() {
        return start == null ? Strings.EMPTY : start.toString();
    }

    @Nullable
    public String end() {
        return end == null ? Strings.EMPTY : end.toString();
    }
}
