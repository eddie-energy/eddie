package energy.eddie.aiida.schemas;

import energy.eddie.aiida.errors.formatter.SchemaFormatterRegistryException;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SchemaFormatterRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaFormatterRegistry.class);
    private final Map<AiidaSchema, SchemaFormatter> activeFormatters = new EnumMap<>(AiidaSchema.class);


    public SchemaFormatterRegistry(List<SchemaFormatter> schemaFormatters) {
        for (SchemaFormatter schemaFormatter : schemaFormatters) {
            if (AnnotationUtils.findAnnotation(schemaFormatter.getClass(), Deprecated.class) != null) {
                LOGGER.debug("Skipping deprecated formatter: {}", schemaFormatter.getClass().getName());
                continue;
            }

            var schema = schemaFormatter.supportedSchema();
            activeFormatters.putIfAbsent(schema, schemaFormatter);
        }

        LOGGER.debug("Registered {} formatters", activeFormatters.size());
    }

    public SchemaFormatter formatterFor(AiidaSchema schema) throws SchemaFormatterRegistryException {
        if (activeFormatters.containsKey(schema)) {
            return activeFormatters.get(schema);
        }

        throw new SchemaFormatterRegistryException(schema);
    }
}
