// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.dataneeds.needs.DataNeed;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AiidaDataNeedAssetsTest {
    private static final String TEMPLATE = """
            {
              "type": "%s",
              "id": "c1a4cb5a-4b2b-4d6c-9a3c-1f2e3d4c5b6a",
              "name": "AIIDA data need",
              "description": "Description",
              "purpose": "Purpose",
              "policyLink": "https://example.com/toc",
              "duration": {"type": "relativeDuration", "durationStart": "P0D", "durationEnd": "P10D"},
              "transmissionSchedule": "*/5 * * * * *",
              "schemas": ["SMART-METER-P1-RAW"]%s
            }
            """;
    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void givenMissingAssets_whenDeserialize_thenAssetsAreEmpty() {
        // When
        var dataNeed = read("");

        // Then
        assertEquals(Set.of(), dataNeed.assets());
    }

    @Test
    void givenNullAssets_whenDeserialize_thenAssetsAreEmpty() {
        // When
        var dataNeed = read(", \"assets\": null");

        // Then
        assertEquals(Set.of(), dataNeed.assets());
    }

    @Test
    void givenMultipleAssets_whenDeserialize_thenContainsThoseAssets() {
        // When
        var dataNeed = read(", \"assets\": [\"CONNECTION-AGREEMENT-POINT\", \"SUBMETER\"]");

        // Then
        assertEquals(Set.of(AiidaAsset.CONNECTION_AGREEMENT_POINT, AiidaAsset.SUBMETER), dataNeed.assets());
    }

    @Test
    void givenMultipleAssets_whenSerialize_thenAssetsAreWrittenAsList() {
        // Given
        var dataNeed = read(", \"assets\": [\"SUBMETER\"]");

        // When
        var json = mapper.writeValueAsString(dataNeed);

        // Then
        assertTrue(json.contains("\"assets\":[\"SUBMETER\"]"), json);
        assertFalse(json.contains("\"asset\":"), json);
    }

    private OutboundAiidaDataNeed read(String assetsJson) {
        return (OutboundAiidaDataNeed) mapper.readValue(TEMPLATE.formatted(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE,
                                                                           assetsJson), DataNeed.class);
    }
}
