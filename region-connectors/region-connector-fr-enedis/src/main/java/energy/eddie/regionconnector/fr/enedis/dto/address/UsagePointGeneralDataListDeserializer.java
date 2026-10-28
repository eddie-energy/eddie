// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.address;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the données générales response whether Enedis wraps it in an array or sends a bare object.
 * <p>
 * The published Swagger documents an object, but the sibling situation contractuelle API turned out
 * to return an array despite its own documentation, so both shapes are accepted.
 */
public class UsagePointGeneralDataListDeserializer extends ValueDeserializer<List<UsagePointGeneralData>> {

    @Override
    public List<UsagePointGeneralData> deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return List.of();
            }
            return List.of(ctxt.readValue(p, UsagePointGeneralData.class));
        }

        List<UsagePointGeneralData> elements = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            elements.add(ctxt.readValue(p, UsagePointGeneralData.class));
        }
        return elements;
    }
}