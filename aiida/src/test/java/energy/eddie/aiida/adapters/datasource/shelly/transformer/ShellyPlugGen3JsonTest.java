// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static org.junit.jupiter.api.Assertions.*;

public class ShellyPlugGen3JsonTest {
    public static final String SWITCH_PAYLOAD = """
            {
              "src": "shelly1pmg4-8813bfe14804",
              "dst": "aiida/17a019b1-253b-40de-8800-d577e4e96181/events",
              "method": "NotifyStatus",
              "params": {
                "ts": 1757591520.01,
                "switch:0": {
                  "aenergy": {
                    "by_minute": [
                      0,
                      0,
                      0
                    ],
                    "minute_ts": 1757591520,
                    "total": 1235.456
                  },
                  "apower": 35,
                  "current": 77,
                  "freq": 50.01,
                  "id": 0,
                  "output": true,
                  "ret_aenergy": {
                    "by_minute": [
                      312.345,
                      23.456,
                      356.789
                    ],
                    "minute_ts": 1757591520,
                    "total": 234.567
                  },
                  "voltage": 228.5
                }
              }
            }
            """;

    @Test
    void deserialize_returnsSwitchJson() throws JacksonException {
        var objectMapper = ObjectMapperCreatorUtil.mapper();

        var json = objectMapper.readValue(SWITCH_PAYLOAD, ShellyPlugGen3Json.class);
        var switchData = json.params().switch0();

        assertNotNull(switchData);
        assertEquals("shelly1pmg4-8813bfe14804", json.source());
        assertEquals("aiida/17a019b1-253b-40de-8800-d577e4e96181/events", json.destination());
        assertEquals("NotifyStatus", json.method());
        assertEquals(1757591520.01, json.params().timestamp());
        assertEquals(77, switchData.current());
        assertEquals(35, switchData.apower());
        assertEquals(228.5, switchData.voltage());
        assertEquals(50.01, switchData.freq());
        assertEquals(1235.456, switchData.aenergy().total());
    }

    @Test
    void deserialize_returnsJsonWithNullSwitch_whenNoSwitchData() throws JacksonException {
        var objectMapper = ObjectMapperCreatorUtil.mapper();
        var noSwitchPayload = """
                {
                  "src": "shelly1pmg4-8813bfe14804",
                  "dst": "aiida/17a019b1-253b-40de-8800-d577e4e96181/events",
                  "method": "NotifyStatus",
                  "params": {
                    "ts": 1757591520.01
                  }
                }
                """;

        var json = objectMapper.readValue(noSwitchPayload, ShellyPlugGen3Json.class);

        assertNotNull(json);
        assertNull(json.params().switch0());
    }

    @Test
    void deserialize_throws_whenJsonIsInvalid() {
        var objectMapper = ObjectMapperCreatorUtil.mapper();
      var invalidJson = "{ invalid json }";

        assertThrows(JacksonException.class, () -> objectMapper.readValue(invalidJson, ShellyPlugGen3Json.class));
    }
}
// This test class verifies the deserialization of JSON payloads specific to the Shelly Plug Gen 3 device. 
//It checks that valid JSON is correctly mapped to the ShellyPlugGen3Json record, including nested switch data, and that invalid JSON results in an appropriate exception being thrown. 
//It extends the test coverage for the Shelly Plug Gen 3 adapter by ensuring that the JSON structure is correctly handled before being processed into measurements and ensures compatibility for future changes in the JSON structure.