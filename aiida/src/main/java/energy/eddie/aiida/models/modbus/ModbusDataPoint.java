package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ModbusDataPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModbusDataPoint.class);
    private static final int EMPTY_REGISTER_COUNT = 0;
    private static final int DEFAULT_REGISTER_COUNT = 1;
    private static final int TWO_REGISTER_COUNT = 2;

    private final String id;
    private final int register;
    private final RegisterType registerType;
    private final int length;
    private final String valueType;
    private final Endian endian;
    private final boolean virtual;
    private final List<String> sources;
    private final String transform;
    private final Map<String, String> translations;
    private final Access access;
    private ObisCode obisCode = ObisCode.UNKNOWN;
    private UnitOfMeasurement unitOfMeasurement = UnitOfMeasurement.UNKNOWN;

    @JsonCreator
    public ModbusDataPoint(
            @JsonProperty("id") String id,
            @JsonProperty("register") int register,
            @JsonProperty("registerType") RegisterType registerType,
            @JsonProperty("length") int length,
            @JsonProperty("valueType") String valueType,
            @JsonProperty("endian") Endian endian,
            @JsonProperty("virtual") boolean virtual,
            @JsonProperty("source") List<String> source,
            @JsonProperty("transform") String transform,
            @JsonProperty("translations") Map<String, String> translations,
            @JsonProperty("access") Access access
    ) {
        this.id = id;
        this.register = register;
        this.registerType = registerType;
        this.length = length;
        this.valueType = valueType;
        this.endian = endian;
        this.virtual = virtual;
        this.sources = source;
        this.transform = transform;
        this.translations = translations;
        this.access = access;
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
        return !(transform == null || sources == null || sources.isEmpty());
    }

    public String id() {
        return id;
    }

    public int register() {
        return register;
    }

    public RegisterType registerType() {
        return registerType;
    }

    public int length() {
        if (length > 0) {
            return length;
        }

        if (registerType == null || valueType == null) {
            return EMPTY_REGISTER_COUNT;
        }

        if (List.of(RegisterType.COIL, RegisterType.DISCRETE).contains(registerType)) {
            return DEFAULT_REGISTER_COUNT;
        }

        return switch (this.valueType.toLowerCase(Locale.ROOT)) {
            case "int16", "uint16" -> DEFAULT_REGISTER_COUNT;
            case "int32", "uint32", "float32" -> TWO_REGISTER_COUNT;
            case "string" -> {
                LOGGER.warn("Length not set for string valueType on datapoint '{}'; returning default 1", id);
                yield DEFAULT_REGISTER_COUNT;
            }
            default -> {
                LOGGER.warn("Unknown or missing valueType '{}' for datapoint '{}'; returning default 1",
                            valueType,
                            id);
                yield DEFAULT_REGISTER_COUNT;
            }
        };
    }

    public String valueType() {
        return valueType;
    }

    public Endian endian() {
        return endian;
    }

    public boolean virtual() {
        return virtual;
    }

    public List<String> sources() {
        return sources;
    }

    public String transform() {
        return transform;
    }

    public Map<String, String> translations() {
        return translations;
    }

    public Access access() {
        return access;
    }

    public ObisCode obisCode() {
        return obisCode;
    }

    public UnitOfMeasurement unitOfMeasurement() {
        return unitOfMeasurement;
    }

    @JsonSetter("obisCode")
    protected void setObisCode(ObisCode obisCode) {
        if (obisCode != null) {
            this.obisCode = obisCode;
        }
    }

    @JsonSetter("unit")
    protected void setUnitOfMeasurement(UnitOfMeasurement unitOfMeasurement) {
        if (unitOfMeasurement != null) {
            this.unitOfMeasurement = unitOfMeasurement;
        }
    }
}
