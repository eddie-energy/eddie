package energy.eddie.aiida.errors.formatter;

import energy.eddie.dataneeds.needs.aiida.AiidaSchema;

public class SchemaFormatterRegistryException extends Exception {
    public SchemaFormatterRegistryException(AiidaSchema aiidaSchema) {
        super("No SchemaFormatter found for schema: " + aiidaSchema);
    }
}
