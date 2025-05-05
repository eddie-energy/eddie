package energy.eddie.aiida.models.datasource.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceModbusDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.IntervalBasedDataSource;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import javax.annotation.Nullable;
import java.util.Optional;
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

    public ModbusDataSource(DataSourceDto dto, UUID userId, DataSourceModbusDto modbusDto) {
        super(dto, userId);
        this.modbusIp = modbusDto.modbusIp();
        this.modbusDevice = modbusDto.modbusDevice();
        this.modbusVendor = modbusDto.modbusVendor();
        this.modbusModel = modbusDto.modbusModel();
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

    @Override
    public DataSourceDto toDto() {
        return new DataSourceDto(
                id,
                dataSourceType,
                asset,
                name,
                enabled,
                pollingInterval,
                null,
                new DataSourceModbusDto(modbusIp, modbusVendor, modbusModel, modbusDevice)
        );
    }


    @Override
    public DataSource mergeWithDto(DataSourceDto dto, UUID userId) {
        String updatedIp = Optional.ofNullable(dto.modbusSettings())
                .map(DataSourceModbusDto::modbusIp)
                .orElse(this.modbusIp());

        if (updatedIp == null) {
            throw new IllegalArgumentException("Modbus IP is required and must not be null");
        }

        return createFromDto(dto, userId, new DataSourceModbusDto(updatedIp, this.modbusVendor(), this.modbusModel(), this.modbusDevice()));
    }
}
