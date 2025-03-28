package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ModbusSource {

    private final SourceCategory category;
    private String id;
    private final List<ModbusDataPoint> datapoints;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ModbusSource(
            @JsonProperty("category") SourceCategory category,
            @JsonProperty("id") String id,
            @JsonProperty("datapoints") List<ModbusDataPoint> datapoints
    ) {
        this.category = category;
        this.id = id;
        this.datapoints = datapoints;
    }

    public SourceCategory getCategory() {
        return category;
    }

    public List<ModbusDataPoint> getDatapoints() {
        return datapoints;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}


