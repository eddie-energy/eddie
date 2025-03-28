package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ModbusDataPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModbusDataPoint.class);
    private String id;
    private final int register;
    private int length;
    private final RegisterType registerType;
    private final String valueType;
    private Endian endian;
    private final boolean virtual;
    private List<String> source;
    private final String transform;
    private final Map<String, String> translations;
    private final Access access;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
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
        this.length = length;
        this.registerType = registerType;
        this.valueType = valueType;
        this.endian = endian;
        this.virtual = virtual;
        this.source = source;
        this.transform = transform;
        this.translations = translations;
        this.access = access;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getRegister() {
        return register;
    }

    public int getLength() {
        if (this.length > 0) {
            return this.length;
        }

        if (this.registerType == null || this.valueType == null) {
            return 0; // to be noticed
        }

        if (List.of(RegisterType.COIL, RegisterType.DISCRETE).contains(this.registerType)) {
            return 1;
        }

        return switch (this.valueType.toLowerCase(Locale.ROOT)) {
            case "int16", "uint16" -> 1;
            case "int32", "uint32", "float32" -> 2;
            case "string" -> {
                LOGGER.warn("Length not set for string valueType on datapoint '{}'; returning default 1", this.id);
                yield 1; // Or throw exception?
            }
            default -> {
                LOGGER.warn("Unknown or missing valueType '{}' for datapoint '{}'; returning default 1", this.valueType, this.id);
                yield 1;
            }
        };
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getValueType() {
        return valueType;
    }

    public Endian getEndian() {
        return endian;
    }

    public void setEndian(Endian endian) {
        this.endian = endian;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public List<String> getSource() {
        return source;
    }

    public void setSource(List<String> source) {
        this.source = source;
    }

    public String getTransform() {
        return transform;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public RegisterType getRegisterType() {
        return registerType;
    }

    public Access getAccess() {
        return access;
    }
}

