package energy.eddie.aiida.schemas;

import energy.eddie.aiida.errors.formatter.FormatterException;
import energy.eddie.aiida.errors.formatter.RawFormatterException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class RawFormatter extends SchemaFormatter {

    @Override
    public byte[] toSchema(
            AiidaRecord aiidaRecord,
            ObjectMapper objectMapper,
            Permission ignored
    ) throws FormatterException {
        try {
            return objectMapper.writeValueAsBytes(aiidaRecord);
        } catch (JacksonException e) {
            throw new RawFormatterException(e);
        }
    }
}
