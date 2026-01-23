// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.client.deserializer;

import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerTransaction;
import jakarta.annotation.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

public class CustomerTransactionDeserializer extends StdDeserializer<CustomerTransaction> {
    protected CustomerTransactionDeserializer() {
        super(CustomerTransaction.class);
    }

    @Override
    @Nullable
    public CustomerTransaction deserialize(
            JsonParser p,
            DeserializationContext ctx
    ) {
        JsonNode node = p.readValueAsTree();

        if (node.isEmpty()) {
            return null;
        }
        return ctx.readTreeAsValue(node, CustomerTransaction.class);
    }
}
