package energy.eddie.regionconnector.fi.fingrid.client.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerTransaction;
import jakarta.annotation.Nullable;

import java.io.IOException;

public class CustomerTransactionDeserializer extends JsonDeserializer<CustomerTransaction> {
    @Override
    @Nullable
    public CustomerTransaction deserialize(
            JsonParser p,
            DeserializationContext ctx
    ) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.isEmpty()) {
            return null;
        }
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        return mapper.treeToValue(node, CustomerTransaction.class);
    }
}
