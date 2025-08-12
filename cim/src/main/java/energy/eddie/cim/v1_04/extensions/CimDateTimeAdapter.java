package energy.eddie.cim.v1_04.extensions;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

public class CimDateTimeAdapter extends XmlAdapter<String, ZonedDateTime> {

    @Override
    public ZonedDateTime unmarshal(String stringValue) {
        return stringValue != null ? ZonedDateTime.parse(stringValue, ISO_DATE_TIME) : null;
    }

    @Override
    public String marshal(ZonedDateTime value) {
        if (value == null) {
            return null;
        }
        return ISO_DATE_TIME.format(value.truncatedTo(ChronoUnit.SECONDS).withZoneSameInstant(ZoneOffset.UTC));
    }
}