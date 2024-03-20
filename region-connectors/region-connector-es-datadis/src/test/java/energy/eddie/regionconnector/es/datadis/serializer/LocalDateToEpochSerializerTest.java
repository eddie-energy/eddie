package energy.eddie.regionconnector.es.datadis.serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class LocalDateToEpochSerializerTest {

    @Test
    void serialize_returnsExpected() throws IOException {
        LocalDateToEpochSerializer serializer = new LocalDateToEpochSerializer();
        SerializerProvider serializerProvider = mock(SerializerProvider.class);
        StringWriter stringWriter = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(stringWriter);

        LocalDate localDate = LocalDate.of(2023, 9, 19);
        long expectedTimestamp = localDate.atStartOfDay(ZONE_ID_SPAIN).toInstant().toEpochMilli();

        serializer.serialize(localDate, jsonGenerator, serializerProvider);
        jsonGenerator.flush();

        String output = stringWriter.toString();
        assertEquals(Long.toString(expectedTimestamp), output);
    }
}