package energy.eddie.regionconnector.shared.event.sourcing.converters;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Serializes and deserializes the {@link AttributeError} to a JSON list in order to persist it to a database column.
 */
@Converter
public class AttributeErrorListConverter implements AttributeConverter<List<AttributeError>, String> {
    private static final ObjectMapper objectMapper = JsonMapper.builder()
                                                               .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                                               .build();

    @Override
    @Nullable
    public String convertToDatabaseColumn(List<AttributeError> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException je) {
            throw new AttributeErrorListConverterException(je);
        }
    }

    @Override
    @Nullable
    public List<AttributeError> convertToEntityAttribute(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JacksonException je) {
            throw new AttributeErrorListConverterException(je);
        }
    }
}
