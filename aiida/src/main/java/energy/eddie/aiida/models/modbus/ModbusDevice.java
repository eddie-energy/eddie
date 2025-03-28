package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ModbusDevice {
    private String id;
    private String name;
    private final int port;
    private final int unitId;
    private final Intervals intervals;
    private final List<ModbusSource> sources;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ModbusDevice(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("port") int port,
            @JsonProperty("unitId") int unitId,
            @JsonProperty("intervals") Intervals intervals,
            @JsonProperty("sources") List<ModbusSource> sources
    ) {
        this.id = id;
        this.name = name;
        this.port = port;
        this.unitId = unitId;
        this.intervals = intervals;
        this.sources = sources;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public int getUnitId() {
        return unitId;
    }

    public Intervals getIntervals() {
        return intervals;
    }

    public List<ModbusSource> getSources() {
        return sources;
    }
}
