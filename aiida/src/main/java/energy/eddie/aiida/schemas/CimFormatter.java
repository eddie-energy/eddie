package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.record.AiidaRecord;
import org.apache.commons.lang3.NotImplementedException;

public class CimFormatter extends SchemaFormatter {

    @Override
    public byte[] toSchema(AiidaRecord aiidaRecord, ObjectMapper mapper) {
        throw new NotImplementedException();
    }
}
