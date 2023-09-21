package energy.eddie.regionconnector.es.datadis.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

public class LocalDateToEpochSerializer extends JsonSerializer<LocalDate> {
    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        long timestamp = value.atStartOfDay(ZONE_ID_SPAIN).toInstant().toEpochMilli();
        gen.writeNumber(timestamp);
    }
}
