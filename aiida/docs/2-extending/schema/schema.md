# Add a schema

To create a new schema in AIIDA, typically the following components are required:

- [Schema Type](#schema-type)
- [Formatter Exception](#formatter-exception)
- [Schema Formatter](#schema-formatter)
- [Region Connector](#region-connector)

## Schema Type

To later parse the needed schema from the DataNeed, it has to be defined in the [AiidaSchema](https://github.com/eddie-energy/eddie/blob/main/api/src/main/java/energy/eddie/api/agnostic/aiida/AiidaSchema.java) enum.
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
This exception must extend from the abstract [SchemaFormatterException](https://github.com/eddie-energy/eddie/blob/main/aiida/src/main/java/energy/eddie/aiida/errors/formatter/SchemaFormatterException.java) class.

```java
package energy.eddie.aiida.errors.formatter;

public class SchemaFormatterException extends FormatterException {
    public SchemaFormatterException(Exception exception) {
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

The logic of the `CustomFormatter` has to be implemented.

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

## Region Connector

> `region-connectors/region-connector-aiida`

### 1. Add Sink bean

Add a new bean for the Sink in the `AiidaBeanConfig` class.

```java
@Bean
public Sinks.Many<RTDEnvelope> nearRealTimeDataSink() {
    return Sinks.many().multicast().onBackpressureBuffer();
}
```

### 2. Integrate Sink in `MqttMessageCallback`

Inject the new sink into the constructor of the `MqttMessageCallback` class.
Extend this class to support the new schema.
Update the `messageArrived` method to parse messages of the new schema and emit them to the sink.

### 3. Register Sink in `IdentifiableStreams`

Add the newly created sink to the `IdentifiableStreams` class so that it can be accessed throughout the application.

### 4. Create Schema Provider

Each schema requires a dedicated provider that implements the corresponding interface from the `api` package.
This provider retrieves the `IdentifiableStreams` bean and exposes the sink as a `Flux`.

Example implementation:

```java
@Component
public class AiidaNearRealTimeDataMarketDocumentProvider implements NearRealTimeDataMarketDocumentProvider {
    private final Flux<RTDEnvelope> flux;

    public AiidaNearRealTimeDataMarketDocumentProvider(IdentifiableStreams streams) {
        this.flux = streams.nearRealTimeDataFlux();
    }

    @Override
    public Flux<RTDEnvelope> getNearRealTimeMarketDocumentsStream() {
        return flux;
    }
}
```

### 5. Continue with CIM Integration

Continue with CIM Integration [CIM README](https://github.com/eddie-energy/eddie/blob/ce4ae20303bcdae7d247c607a89727a82e8c4865/cim/README.md)