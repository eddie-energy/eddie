package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ModbusSource(
        SourceCategory category,
        String id,
        @JsonProperty("datapoints")
        List<ModbusDataPoint> dataPoints
) {}
