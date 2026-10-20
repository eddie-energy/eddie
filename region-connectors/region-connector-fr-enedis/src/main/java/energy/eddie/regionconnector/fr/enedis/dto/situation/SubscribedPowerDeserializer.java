// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Reads {@code subscribed_power} from {@code situation_contrat_auto} whether Enedis returns a bare
 * scalar ({@code "6"}) or the {@code power} object its Swagger defines ({@code {"unit": "kW",
 * "value": "6"}}). In the object case the {@code value} is returned, which is the only part a
 * downstream consumer can use as a quantity.
 */
public class SubscribedPowerDeserializer extends ValueDeserializer<String> {

    @Override
    public @Nullable String deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return p.getString();
        }
        if (p.currentToken() != JsonToken.START_OBJECT) {
            return null;
        }

        String value = null;
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();
            if ("value".equals(field) && p.currentToken() == JsonToken.VALUE_STRING) {
                value = p.getString();
            } else {
                p.skipChildren();
            }
        }
        return value;
    }
}