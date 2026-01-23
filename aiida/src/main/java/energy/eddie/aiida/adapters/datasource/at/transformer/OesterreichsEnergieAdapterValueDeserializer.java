// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.at.transformer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

public class OesterreichsEnergieAdapterValueDeserializer extends StdDeserializer<OesterreichsEnergieAdapterJson.AdapterValue> {
    public OesterreichsEnergieAdapterValueDeserializer() {
        super(OesterreichsEnergieAdapterJson.AdapterValue.class);
    }

    @Override
    public OesterreichsEnergieAdapterJson.AdapterValue deserialize(JsonParser jp, DeserializationContext context) {
        JsonNode node = jp.readValueAsTree();

        // use value either as Integer or String
        JsonNode valueNode = node.get("value");
        Object value = valueNode.isInt()
                ? valueNode.intValue()
                : valueNode.asString();

        JsonNode timeNode = node.get("time");
        long time = timeNode != null
                ? timeNode.longValue()
                : 0;

        return new OesterreichsEnergieAdapterJson.AdapterValue(value, time);
    }
}
