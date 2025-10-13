package energy.eddie.aiida.models.datasource.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.modbus.ModbusDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.IntervalBasedDataSource;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.MODBUS_TCP)
public class ModbusDataSource extends IntervalBasedDataSource {

    @JsonProperty
    @Column(name = "modbus_ip")
    private String modbusIp;

    @JsonProperty
    @Nullable
    @Column(name = "modbus_device")
    private UUID modbusDevice;

    @JsonProperty
    @Nullable
    @Column(name = "modbus_vendor")
    private UUID modbusVendor;

    @JsonProperty
    @Nullable
    @Column(name = "modbus_model")
    private UUID modbusModel;

    protected ModbusDataSource() {
        this.modbusIp = "";
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

    private void applyDto(ModbusDataSourceDto dto) {
        this.modbusIp = dto.modbusIp();
        this.modbusDevice = dto.modbusDevice();
        this.modbusVendor = dto.modbusVendor();
        this.modbusModel = dto.modbusModel();
    }

    @Nullable
    public String modbusIp() {
        return modbusIp;
    }

    @Nullable
    public UUID modbusVendor() {
        return modbusVendor;
    }

    @Nullable
    public UUID modbusModel() {
        return modbusModel;
    }

    @Nullable
    public UUID modbusDevice() {
        return modbusDevice;
    }
}
