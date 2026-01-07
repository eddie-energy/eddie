package energy.eddie.aiida.adapters.datasource.it.transformer;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SinapsiAlfaEntryJsonTest {
    private static final String PAYLOAD_ENTRY = """
            {
              "du": "DU12345678",
              "pod": "IT00123456789A",
              "data": [
                {
                  "ts": "1757506202",
                  "1-0:1.7.0.255_3,0_2": "59"
                }
              ]
            }
            """;
    public static final String PAYLOAD = "[" + PAYLOAD_ENTRY + "]";

    @Test
    void deserialize_returnsParsed() throws JacksonException {
        var objectMapper = ObjectMapperCreatorUtil.mapper();

        var json = objectMapper.readValue(PAYLOAD_ENTRY, SinapsiAlfaEntryJson.class);

        assertEquals("DU12345678", json.meterFabricationNumber());
        assertEquals("IT00123456789A", json.pointOfDelivery());
        assertEquals(59, json.data().getFirst().entries().get("1-0:1.7.0.255_3,0_2"));
    }
}
