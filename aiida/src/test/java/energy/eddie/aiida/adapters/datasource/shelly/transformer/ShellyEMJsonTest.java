package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import energy.eddie.aiida.config.AiidaConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ShellyEMJsonTest {
    public static final String EM_PAYLOAD = """
            {
              "src": "shellypro3em-8813bfe14804",
              "dst": "aiida/17a019b1-253b-40de-8800-d577e4e96181/events",
              "method": "NotifyStatus",
              "params": {
                "ts": 1757059829.08,
                "em:0": {
                  "id": 0,
                  "a_act_power": 0,
                  "a_aprt_power": 0,
                  "a_current": 0.027,
                  "a_freq": 198,
                  "a_pf": 1,
                  "a_voltage": 0,
                  "b_act_power": 0,
                  "b_aprt_power": 0,
                  "b_current": 0.027,
                  "b_freq": 160.7,
                  "b_pf": 1,
                  "b_voltage": 0,
                  "c_act_power": -18,
                  "c_aprt_power": 108.7,
                  "c_current": 0.476,
                  "c_freq": 50,
                  "c_pf": 0.55,
                  "c_voltage": 228.3,
                  "n_current": null,
                  "total_act_power": -18.017,
                  "total_aprt_power": 108.708,
                  "total_current": 0.531
                }
              }
            }
            """;
    private static final String EM_DATA_PAYLOAD = """
            {
              "src": "shellypro3em-8813bfe14804",
              "dst": "aiida/17a019b1-253b-40de-8800-d577e4e96181/events",
              "method": "NotifyStatus",
              "params": {
                "ts": 1757060280.21,
                "emdata:0": {
                  "id": 0,
                  "a_total_act_energy": 0.06,
                  "a_total_act_ret_energy": 0,
                  "b_total_act_energy": 0.06,
                  "b_total_act_ret_energy": 0.01,
                  "c_total_act_energy": 26289.44,
                  "c_total_act_ret_energy": 131881.85,
                  "total_act": 26289.57,
                  "total_act_ret": 131881.86
                }
              }
            }
            """;
    private static final String UNKNOWN_COMPONENT_PAYLOAD = """
            {
              "src": "shellypro3em-8813bfe14804",
              "dst": "aiida/17a019b1-253b-40de-8800-d577e4e96181/events",
              "method": "NotifyStatus",
              "params": {
                "ts": 1757060280.21,
                "unknown:0": {
                  "id": 0,
                  "some_value": 12345
                }
              }
            }
            """;

    @Test
    void deserialize_returnsEMJson() throws JsonProcessingException {
        var objectMapper = new AiidaConfiguration().customObjectMapper().build();

        var json = objectMapper.readValue(EM_PAYLOAD, ShellyEMJson.class);
        var component = json.params().em().get(ShellyEMComponent.EM);

        assertNotNull(component);
        assertEquals("shellypro3em-8813bfe14804", json.source());
        assertEquals("aiida/17a019b1-253b-40de-8800-d577e4e96181/events", json.destination());
        assertEquals("NotifyStatus", json.method());
        assertEquals(1757059829.08, json.params().timestamp());
        assertEquals(0.027, component.get("a_current"));
        assertEquals(-18.017, component.get("total_act_power"));
    }

    @Test
    void deserialize_returnsEMDataJson() throws JsonProcessingException {
        var objectMapper = new AiidaConfiguration().customObjectMapper().build();

        var json = objectMapper.readValue(EM_DATA_PAYLOAD, ShellyEMJson.class);
        var component = json.params().em().get(ShellyEMComponent.EM_DATA);

        assertNotNull(component);
        assertEquals("shellypro3em-8813bfe14804", json.source());
        assertEquals("aiida/17a019b1-253b-40de-8800-d577e4e96181/events", json.destination());
        assertEquals("NotifyStatus", json.method());
        assertEquals(1757060280.21, json.params().timestamp());
        assertEquals(26289.57, component.get("total_act"));
        assertEquals(131881.86, component.get("total_act_ret"));
    }

    @Test
    void deserialize_returnsJsonWithoutData_whenUnknownComponent() throws JsonProcessingException {
        var objectMapper = new AiidaConfiguration().customObjectMapper().build();

        var json = objectMapper.readValue(UNKNOWN_COMPONENT_PAYLOAD, ShellyEMJson.class);

        assertNotNull(json);
        assertEquals("shellypro3em-8813bfe14804", json.source());
        assertEquals("aiida/17a019b1-253b-40de-8800-d577e4e96181/events", json.destination());
        assertEquals("NotifyStatus", json.method());
        assertEquals(1757060280.21, json.params().timestamp());
        assertTrue(json.params().em().isEmpty());
    }

    @Test
    void deserialize_throws_whenJsonIsInvalid() {
        var objectMapper = new AiidaConfiguration().customObjectMapper().build();
        var invalidJson = "{ \"foo\": \"bar\" }";

        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(invalidJson, ShellyEMJson.class));
    }
}
