package energy.eddie.aiida.adapters.datasource.it.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import energy.eddie.aiida.config.AiidaConfiguration;
import org.junit.jupiter.api.Test;

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
    void deserialize_returnsParsed() throws JsonProcessingException {
        var objectMapper = new AiidaConfiguration().customObjectMapper().build();

        var json = objectMapper.readValue(PAYLOAD_ENTRY, SinapsiAlfaEntryJson.class);

        assertEquals("DU12345678", json.meterFabricationNumber());
        assertEquals("IT00123456789A", json.pointOfDelivery());
        assertEquals(59, json.data().getFirst().entries().get("1-0:1.7.0.255_3,0_2"));
    }
}
