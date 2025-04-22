package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ModbusDevice(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("port") int port,
        @JsonProperty("unitId") int unitId,
        @JsonProperty("intervals") Intervals intervals,
        @JsonProperty("sources") List<ModbusSource> sources
) {}