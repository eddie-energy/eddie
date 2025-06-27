package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;

import java.util.UUID;

public abstract class SchemaFormatter {

    public static SchemaFormatter getFormatter(UUID aiidaId, AiidaSchema schema) {
        return switch (schema) {
            case SMART_METER_P1_CIM -> new CimFormatter(aiidaId);
            case SMART_METER_P1_RAW -> new RawFormatter();
        };
    }

    public abstract byte[] toSchema(AiidaRecord aiidaRecord, ObjectMapper mapper, Permission permission);
}
