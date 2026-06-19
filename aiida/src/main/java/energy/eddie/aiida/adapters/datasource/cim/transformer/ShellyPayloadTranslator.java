// SPDX-FileCopyrightText: 2026 The EDDIE Developers
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.cim.transformer;

import energy.eddie.aiida.adapters.datasource.cim.transformer.ShellyToAiidaTransformer;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyJson;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

public class ShellyPayloadTranslator implements PayloadToAiidaTranslator {
    private final ObjectMapper mapper;

    public ShellyPayloadTranslator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<List<AiidaRecordValue>> tryTranslate(String payload) {
        try {
            // Try parsing the payload as-is
            var shellyJson = mapper.readValue(payload, ShellyJson.class);

            return Optional.of(ShellyToAiidaTransformer.transform(shellyJson));
        } catch (JacksonException e) {
            // Some Shelly MQTT messages are prefixed with human-readable text like
            // "shelly_notification:163 Status change of switch:0: { ... }". In that
            // case, attempt to extract the JSON substring starting at the first '{'.
            int idx = payload.indexOf('{');
            if (idx >= 0) {
                String jsonSub = payload.substring(idx).trim();
                try {
                    var shellyJson = mapper.readValue(jsonSub, ShellyJson.class);

                    return Optional.of(ShellyToAiidaTransformer.transform(shellyJson));
                } catch (JacksonException ex) {
                    return Optional.empty();
                }
            }

            return Optional.empty();
        }
    }
}
