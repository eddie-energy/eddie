package energy.eddie.regionconnector.at.eda.permission.request.events.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class AttributeErrorListConverter implements AttributeConverter<List<AttributeError>, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Nullable
    public String convertToDatabaseColumn(List<AttributeError> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException jpe) {
            throw new AttributeErrorListConverterException(jpe);
        }
    }

    @Override
    @Nullable
    public List<AttributeError> convertToEntityAttribute(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException jpe) {
            throw new AttributeErrorListConverterException(jpe);
        }
    }
}
