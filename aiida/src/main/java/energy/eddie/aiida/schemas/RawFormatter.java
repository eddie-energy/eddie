package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.record.AiidaRecord;

public class RawFormatter extends SchemaFormatter {

    @Override
    public byte[] toSchema(AiidaRecord aiidaRecord, ObjectMapper objectMapper)  {
        try {
            return objectMapper.writeValueAsBytes(aiidaRecord);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
