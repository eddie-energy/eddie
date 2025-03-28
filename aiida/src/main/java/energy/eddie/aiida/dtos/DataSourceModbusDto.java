package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.UUID;

public record DataSourceModbusDto(
        @JsonProperty String modbusIp,
        @Nullable @JsonProperty UUID modbusVendor,
        @Nullable @JsonProperty UUID modbusModel,
        @Nullable @JsonProperty UUID modbusDevice
) implements DataSourceProtocolSettings {
    public DataSourceModbusDto() {
        this("", null, null, null);
    }
}

