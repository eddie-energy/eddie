package energy.eddie.dataneeds.persistence;

import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Period;

@Converter
public class PeriodConverter implements AttributeConverter<Period, String> {
    @Override
    @Nullable
    public String convertToDatabaseColumn(Period period) {
        if (period == null) {
            return null;
        }

        return period.normalized().toString();
    }

    @Override
    @Nullable
    public Period convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        return Period.parse(dbData);
    }
}
