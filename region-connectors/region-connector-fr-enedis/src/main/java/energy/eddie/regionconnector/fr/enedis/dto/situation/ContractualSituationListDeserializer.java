// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the situation contractuelle response whether Enedis wraps it in an array (what it actually
 * returns) or sends a bare object.
 * <p>
 * A type mismatch here aborts the whole master-data fetch and the permission request never reaches a
 * terminal status, so accepting both shapes costs little and keeps the connector working if the
 * wrapper changes.
 */
public class ContractualSituationListDeserializer extends ValueDeserializer<List<ContractualSituation>> {

    @Override
    public List<ContractualSituation> deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            if (p.currentToken() == JsonToken.VALUE_NULL) {
                return List.of();
            }
            return List.of(ctxt.readValue(p, ContractualSituation.class));
        }

        List<ContractualSituation> situations = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            situations.add(ctxt.readValue(p, ContractualSituation.class));
        }
        return situations;
    }
}