// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.at.transformer;

import energy.eddie.aiida.config.AiidaConfiguration;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OesterreichsEnergieAdapterJsonTest {
    /**
     * Checks that the Jackson annotations are correct and every field of the JSON is deserialized as expected.
     */
    @Test
    void verify_isProperlyDeserialized() {
        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        ObjectMapper mapper = builder.build();


        String str = "{\"0-0:96.1.0\":{\"value\":\"90296857\"},\"1-0:1.8.0\":{\"value\":83402,\"time\":1697622940},\"1-0:1.7.0\":{\"value\":43,\"time\":1697622940},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2360.4}";

        var json = mapper.readValue(str, OesterreichsEnergieAdapterJson.class);

        assertEquals("v1", json.apiVersion());
        assertEquals("90296857", json.name());
        assertEquals(2360.4, json.smaTime());

        assertEquals(3, json.energyData().size());

        var first = json.energyData().get("0-0:96.1.0");
        assertNotNull(first);
        assertEquals("90296857", first.value());
        assertEquals(0, first.time());

        var second = json.energyData().get("1-0:1.8.0");
        assertNotNull(second);
        assertEquals(83402, second.value());
        assertEquals(1697622940L, second.time());


        var third = json.energyData().get("1-0:1.7.0");
        assertNotNull(third);
        assertEquals(43, third.value());
        assertEquals(1697622940L, third.time());
    }
}