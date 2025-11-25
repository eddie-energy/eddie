package energy.eddie.aiida.models.datasource.interval.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.modbus.ModbusDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.interval.IntervalDataSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.SecondaryTable;

import java.util.UUID;

@Entity
@SecondaryTable(name = ModbusDataSource.TABLE_NAME)
@DiscriminatorValue(DataSourceType.Identifiers.MODBUS_TCP)
public class ModbusDataSource extends IntervalDataSource {
    protected static final String TABLE_NAME = "data_source_modbus";

    @Column(name = "device_id", table = TABLE_NAME, nullable = false)
    @Schema(description = "The ID of the modbus device.")
    @JsonProperty
    private UUID deviceId;

    @Column(name = "ip_address", table = TABLE_NAME, nullable = false)
    @Schema(description = "The IP Address of the modbus device.")
    @JsonProperty
    private String ipAddress;

    @Column(name = "model_id", table = TABLE_NAME, nullable = false)
    @Schema(description = "The model ID of the modbus device.")
    @JsonProperty
    private UUID modelId;

    @Column(name = "vendor_id", table = TABLE_NAME, nullable = false)
    @Schema(description = "The vendor ID of the modbus device.")
    @JsonProperty
    private UUID vendorId;

    @SuppressWarnings("NullAway")
    protected ModbusDataSource() {
    }

    public ModbusDataSource(ModbusDataSourceDto dto, UUID userId) {
        super(dto, userId);
        applyDto(dto);
    }

    @Override
    public void update(DataSourceDto dto) {
        super.update(dto);
        if (dto instanceof ModbusDataSourceDto modbusDto) {
            applyDto(modbusDto);
        }
    }

    @Nullable
    public String ipAddress() {
        return ipAddress;
    }

    @Nullable
    public UUID deviceId() {
        return deviceId;
    }

    private void applyDto(ModbusDataSourceDto dto) {
        this.deviceId = dto.deviceId();
        this.ipAddress = dto.ipAddress();
        this.modelId = dto.modelId();
        this.vendorId = dto.vendorId();
    }
}
