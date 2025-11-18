package energy.eddie.aiida.adapters.datasource.fr.transformer;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.adapters.datasource.fr.transformer.standard.StandardModeEntry;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public record MicroTeleinfoV3DataField(@JsonProperty String raw,
                                       @JsonProperty Object value,
                                       UnitOfMeasurement unitOfMeasurement,
                                       ObisCode mappedObisCode,
                                       @JsonProperty Optional<MicroTeleinfoV3Timestamp> timestamp
) {
    public Object sanitizedValue(String key) {
        if (StandardModeEntry.needsSanitization(key) && value instanceof String stringValue) {
            return StandardCharsets.UTF_8
                    .decode(StandardCharsets.UTF_8.encode(stringValue.replace("\u0000", "")))
                    .toString();
        }

        return value();
    }
}
