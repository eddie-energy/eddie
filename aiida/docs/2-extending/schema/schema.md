# Add a schema

To create a new schema in AIIDA, typically the following components are required:

- [Schema Type](#schema-type)
- [Formatter Exception](#formatter-exception)
- [Schema Formatter](#schema-formatter)

## Schema Type

To later parse the needed schema from the DataNeed, it has to be defined in the [AiidaSchema](https://github.com/eddie-energy/eddie/blob/main/data-needs/src/main/java/energy/eddie/dataneeds/needs/aiida/AiidaSchema.java) enum.
Add it as enum, like in the example below:

```java
public enum AiidaSchema {
    // ... other schemas
    SMART_METER_P1_CUSTOM("SMART-METER-P1-CUSTOM")

    // ... existing code
}
```

## Formatter Exception

For logging purposes, a custom exception should be created.
This exception must extend from the abstract [FormatterException](https://github.com/eddie-energy/eddie/blob/main/aiida/src/main/java/energy/eddie/aiida/errors/formatter/FormatterException.java) class.

```java
package energy.eddie.aiida.errors.formatter;

public class CustomFormatterException extends FormatterException {
    public CustomFormatterException(Exception exception) {
        super(exception);
    }
}
```

## Schema Formatter

Create a class for the formatter of the newly created schema (e.g. `CustomSchemaFormatter`).
This class must extend from the abstract [SchemaFormatter](https://github.com/eddie-energy/eddie/blob/main/aiida/src/main/java/energy/eddie/aiida/schemas/SchemaFormatter.java) class.

Inside the `SchemaFormatter` class, the subclass must now be mapped to its schema.

```java
public abstract class SchemaFormatter {

    public static SchemaFormatter getFormatter(UUID aiidaId, AiidaSchema schema) {
        return switch (schema) {
            // ... existing schemas
            case SMART_METER_P1_CUSTOM -> new CustomFormatter();
        };
    }

    public abstract byte[] toSchema(
            AiidaRecord aiidaRecord,
            ObjectMapper mapper,
            Permission permission
    ) throws FormatterException;
}
```

Lastly, the logic of the `CustomFormatter` has to be implemented.

```java
public class CustomFormatter extends SchemaFormatter {
    @Override
    public byte[] toSchema(
            AiidaRecord aiidaRecord,
            ObjectMapper objectMapper,
            Permission permission
    ) throws FormatterException {
        try {
            return objectMapper.writeValueAsBytes(customFormatterImplementation(aiidaRecord, permission));
        } catch (JsonProcessingException e) {
            throw new RawFormatterException(e);
        }
    }

    public T customFormatterImplementation(AiidaRecord aiidaRecord, Permission permission) {
        // ... 
    }
}
```