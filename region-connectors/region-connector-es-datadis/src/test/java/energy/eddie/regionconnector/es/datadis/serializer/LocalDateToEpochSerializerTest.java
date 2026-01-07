package energy.eddie.regionconnector.es.datadis.serializer;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

import java.io.StringWriter;
import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalDateToEpochSerializerTest {

    @Test
    void serialize_returnsExpected() {
        LocalDateToEpochSerializer serializer = new LocalDateToEpochSerializer();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(ObjectWriteContext.empty(), stringWriter);

        LocalDate localDate = LocalDate.of(2023, 9, 19);
        long expectedTimestamp = localDate.atStartOfDay(ZONE_ID_SPAIN).toInstant().toEpochMilli();

        serializer.serialize(localDate, jsonGenerator, null);
        jsonGenerator.flush();

        String output = stringWriter.toString();
        assertEquals(Long.toString(expectedTimestamp), output);
    }
}