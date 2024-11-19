package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.dataneeds.validation.schema.AiidaSchema;

public abstract class SchemaFormatter {

    public static SchemaFormatter getFormatter(AiidaSchema schema) {
        return switch (schema) {
            case SMART_METER_P1_CIM -> new CimFormatter();
            case SMART_METER_P1_RAW -> new RawFormatter();
        };
    }

    public abstract byte[] toSchema(AiidaRecord aiidaRecord, ObjectMapper mapper);
}
