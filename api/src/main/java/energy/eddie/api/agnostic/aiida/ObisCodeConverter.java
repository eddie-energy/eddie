package energy.eddie.api.agnostic.aiida;

import jakarta.persistence.AttributeConverter;

public class ObisCodeConverter implements AttributeConverter<ObisCode, String> {
    @Override
    public String convertToDatabaseColumn(ObisCode obisCode) {
        return obisCode.toString();
    }

    @Override
    public ObisCode convertToEntityAttribute(String code) {
        return ObisCode.forCode(code);
    }
}
