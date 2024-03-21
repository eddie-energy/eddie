package energy.eddie.regionconnector.es.datadis.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

public record IntermediateMeteringData(
        List<MeteringData> meteringData,
        ZonedDateTime start,
        ZonedDateTime end
) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static IntermediateMeteringData fromMeteringData(List<MeteringData> meteringData) {
        MeteringData first = meteringData.getFirst();
        MeteringData last = meteringData.getLast();

        ZonedDateTime start = parseDateAndTime(first.date(), first.time());
        ZonedDateTime end = parseDateAndTime(last.date(), last.time());

        return new IntermediateMeteringData(meteringData, start, end);
    }

    private static ZonedDateTime parseDateAndTime(String dateString, String timeString) {
        LocalDate date = LocalDate.parse(dateString, DATE_FORMAT);
        LocalTime time = LocalTime.parse(timeString, TIME_FORMAT);

        // Datadis API returns the time 24:00 for the next day, this is parsed as LocalTime.MIN, but we need to add a day to the date
        if (time.equals(LocalTime.MIN)) {
            date = date.plusDays(1);
        }
        return ZonedDateTime.of(date, time, ZONE_ID_SPAIN);
    }
}
