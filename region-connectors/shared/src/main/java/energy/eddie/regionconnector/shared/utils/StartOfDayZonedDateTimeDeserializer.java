package energy.eddie.regionconnector.shared.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This custom Jackson deserializer converts dates from the connector elements to ZonedDateTime at the start of the day.
 */
public class StartOfDayZonedDateTimeDeserializer extends StdDeserializer<ZonedDateTime> {
    public StartOfDayZonedDateTimeDeserializer() {
        this(null);
    }

    public StartOfDayZonedDateTimeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ZonedDateTime deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        String text = jsonParser.getText();
        return LocalDate.parse(text, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneOffset.UTC);
    }
}
