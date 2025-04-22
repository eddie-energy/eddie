package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@JsonIgnoreProperties({"obis"})
public record ModbusDataPoint(
        String id,
        int register,
        RegisterType registerType,
        int length,
        String valueType,
        Endian endian,
        boolean virtual,
        List<String> source,
        String transform,
        Map<String, String> translations,
        Access access
) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModbusDataPoint.class);

    public int getLength() {
        if (this.length > 0) {
            return this.length;
        }

        if (this.registerType == null || this.valueType == null) {
            return 0;
        }

        if (List.of(RegisterType.COIL, RegisterType.DISCRETE).contains(this.registerType)) {
            return 1;
        }

        return switch (this.valueType.toLowerCase(Locale.ROOT)) {
            case "int16", "uint16" -> 1;
            case "int32", "uint32", "float32" -> 2;
            case "string" -> {
                LOGGER.warn("Length not set for string valueType on datapoint '{}'; returning default 1", this.id);
                yield 1;
            }
            default -> {
                LOGGER.warn("Unknown or missing valueType '{}' for datapoint '{}'; returning default 1", this.valueType, this.id);
                yield 1;
            }
        };
    }

    public Object applyTranslation(Object rawValue) {
        if (translations == null || translations.isEmpty()) {
            return rawValue;
        }

        String rawStr = rawValue.toString();
        return translations.getOrDefault(rawStr, translations.getOrDefault("default", rawStr));
    }

    public Object applyTransform(Object rawValue) {
        if (transform == null || transform.isEmpty()) {
            return rawValue;
        }

        try {
            Map<String, Object> context = new HashMap<>();
            String expr = transform.replace("${self}", "self").replace("@self", "self");
            context.put("self", rawValue);

            return MVEL.eval(expr, context);
        } catch (Exception e) {
            LOGGER.warn("Failed to apply transform '{}' on datapoint {}: {}", transform, id, e.getMessage());
            return rawValue;
        }
    }

    public boolean isValidVirtualDatapoint() {
        return !(this.transform() == null || this.source() == null || this.source().isEmpty());
    }
}
