package energy.eddie.aiida.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Is used to deseralize a JSON date to an Instance of {@code Instant}
 * Following format is necessary to deseralize: "yyyy-MM-dd'T'HH:mm:ss.SSSX"
 */
public class InstantDeserializer extends JsonDeserializer<Instant> {

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                                                           .withZone(ZoneOffset.UTC);
    @Override
    public Instant deserialize(
            JsonParser jsonParser,
            DeserializationContext deserializationContext
    ) throws IOException, DateTimeParseException {
        return Instant.from(fmt.parse(jsonParser.getText()));
    }
}