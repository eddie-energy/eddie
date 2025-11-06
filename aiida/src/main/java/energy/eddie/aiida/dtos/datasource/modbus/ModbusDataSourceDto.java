package energy.eddie.aiida.dtos.datasource.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.IntervalBasedDataSourceDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;

import java.util.UUID;

@SuppressWarnings({"NullAway.Init"})
public class ModbusDataSourceDto extends IntervalBasedDataSourceDto {
    @Column(name = "device_id", nullable = false)
    @Schema(description = "The ID of the modbus device.")
    @JsonProperty
    protected UUID deviceId;

    @Column(name = "ip_address", nullable = false)
    @Schema(description = "The IP address of the modbus device.")
    @JsonProperty
    protected String ipAddress;

    @Column(name = "model_id", nullable = false)
    @Schema(description = "The model ID of the modbus device.")
    @JsonProperty
    protected UUID modelId;

    @Column(name = "vendor_id", nullable = false)
    @Schema(description = "The vendor ID of the modbus device.")
    @JsonProperty
    protected UUID vendorId;

    public UUID deviceId() {
        return deviceId;
    }

    public String ipAddress() {
        return ipAddress;
    }

    public UUID modelId() {
        return modelId;
    }

    public UUID vendorId() {
        return vendorId;
    }
}

