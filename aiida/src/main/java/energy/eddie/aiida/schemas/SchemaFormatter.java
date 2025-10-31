package energy.eddie.aiida.schemas;

import energy.eddie.aiida.errors.formatter.SchemaFormatterException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;

public interface SchemaFormatter {
    AiidaSchema supportedSchema();

    byte[] format(
            AiidaRecord aiidaRecord,
            Permission permission
    ) throws SchemaFormatterException;
}
