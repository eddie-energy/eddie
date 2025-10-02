package energy.eddie.aiida.dtos.datasource.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.IntervalBasedDataSourceDto;

import javax.annotation.Nullable;
import java.util.UUID;

@SuppressWarnings({"NullAway.Init"})
public class ModbusDataSourceDto extends IntervalBasedDataSourceDto {
    @JsonProperty
    protected String modbusIp;
    @Nullable
    @JsonProperty
    protected UUID modbusVendor;
    @Nullable
    @JsonProperty
    protected UUID modbusModelL;
    @Nullable
    @JsonProperty
    protected UUID modbusDevice;

    public String modbusIp() {
        return modbusIp;
    }

    @Nullable
    public UUID modbusVendor() {
        return modbusVendor;
    }

    @Nullable
    public UUID modbusModel() {
        return modbusModelL;
    }

    @Nullable
    public UUID modbusDevice() {
        return modbusDevice;
    }
}

