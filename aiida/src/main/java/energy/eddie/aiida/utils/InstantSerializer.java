package energy.eddie.aiida.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Serializes an Instance of {@code Instant} to json
 * Following format is used: "yyyy-MM-dd'T'HH:mm:ss.SSSX"
 */
public class InstantSerializer extends JsonSerializer<Instant> {

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                                                           .withZone(ZoneOffset.UTC);
    @Override
    public void serialize(
            Instant instant,
            JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider
    ) throws IOException, DateTimeException {
        jsonGenerator.writeString(fmt.format(instant));
    }
}
