package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.record.AiidaRecord;

import java.util.Map;

public abstract class SchemaFormatter {
    public static final Map<String, Class<? extends SchemaFormatter>> SCHEMA_MAP = Map.of(Schemas.SMART_METER_P1_RAW,
                                                                                          RawFormatter.class,
                                                                                          Schemas.SMART_METER_P1_CIM,
                                                                                          CimFormatter.class);

    public static SchemaFormatter getFormatter(String schema) {
        var recordClass = SCHEMA_MAP.get(schema);
        SchemaFormatter schemaFormatter;

        if (recordClass == null) {
            throw new IllegalArgumentException("No implementation for schema: %s found".formatted(schema));
        } else if (recordClass == CimFormatter.class) {
            schemaFormatter = new CimFormatter();
        } else {
            schemaFormatter = new RawFormatter();
        }

        return schemaFormatter;
    }

    public abstract byte[] toSchema(AiidaRecord aiidaRecord, ObjectMapper mapper);
}
