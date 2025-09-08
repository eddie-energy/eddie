package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceModbusDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.dtos.DataSourceProtocolSettings;
import energy.eddie.aiida.models.datasource.modbus.ModbusDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.mqtt.cim.CimDataSource;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.simulation.SimulationDataSource;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import org.springframework.lang.Nullable;

import java.util.UUID;

@Entity
@DiscriminatorColumn(name = "data_source_type", discriminatorType = DiscriminatorType.STRING)
public abstract class DataSource {
    @Id
    @SuppressWarnings({"unused", "NullAway"})
    @JsonProperty
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;
    @Schema(description = "UUID of the user that owns the permission.")
    protected UUID userId;
    @JsonProperty
    @Enumerated(EnumType.STRING)
    protected AiidaAsset asset;
    @JsonProperty
    protected String name;
    @JsonProperty
    protected String countryCode;
    @JsonProperty
    protected boolean enabled;
    @JsonProperty
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source_type", insertable = false, updatable = false)
    protected DataSourceType dataSourceType;

    @SuppressWarnings("NullAway")
    protected DataSource() {}

    protected DataSource(DataSourceDto dto, UUID userId) {
        this.id = dto.id();
        this.userId = userId;
        this.asset = dto.asset();
        this.name = dto.name();
        this.enabled = dto.enabled();
        this.dataSourceType = dto.dataSourceType();
        this.countryCode = dto.countryCode();
    }

    public static DataSource createFromDto(
            DataSourceDto dto,
            UUID userId,
            @Nullable DataSourceProtocolSettings settings
    ) {
        var dataSourceType = dto.dataSourceType();

        return switch (dataSourceType) {
            case SMART_METER_ADAPTER -> {
                if (settings instanceof DataSourceMqttDto mqtt) {
                    yield new OesterreichsEnergieDataSource(dto, userId, mqtt);
                }
                throw createMqttSettingsIllegalStateException(dataSourceType);
            }

            case MICRO_TELEINFO -> {
                if (settings instanceof DataSourceMqttDto mqtt) {
                    yield new MicroTeleinfoV3DataSource(dto, userId, mqtt);
                }
                throw createMqttSettingsIllegalStateException(dataSourceType);
            }

            case SMART_GATEWAYS_ADAPTER -> {
                if (settings instanceof DataSourceMqttDto mqtt) {
                    yield new SmartGatewaysDataSource(dto, userId, mqtt);
                }
                throw createMqttSettingsIllegalStateException(dataSourceType);
            }

            case INBOUND -> {
                if (settings instanceof DataSourceMqttDto mqtt) {
                    yield new InboundDataSource(dto, userId, mqtt);
                }
                throw createMqttSettingsIllegalStateException(dataSourceType);
            }

            case SIMULATION -> new SimulationDataSource(dto, userId);

            case MODBUS -> {
                if (settings instanceof DataSourceModbusDto modbus) {
                    yield new ModbusDataSource(dto, userId, modbus);
                }
                throw new IllegalStateException("Expected MODBUS settings for %s datasource".formatted(dataSourceType));
            }
            case CIM_ADAPTER -> {
                if (settings instanceof DataSourceMqttDto mqtt) {
                    yield new CimDataSource(dto, userId, mqtt);
                }
                throw createMqttSettingsIllegalStateException(dataSourceType);
            }
        };
    }


    public DataSource mergeWithDto(DataSourceDto dto, UUID userId) {
        return createFromDto(dto, userId, new DataSourceMqttDto());
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public AiidaAsset asset() {
        return asset;
    }

    public String name() {
        return name;
    }

    public String countryCode() {
        return countryCode;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DataSourceType dataSourceType() {
        return dataSourceType;
    }

    public DataSourceDto toDto() {
        return new DataSourceDto(id, dataSourceType, asset, name, countryCode, enabled, null, null, null);
    }

    private static IllegalStateException createMqttSettingsIllegalStateException(DataSourceType dataSourceType) {
        return new IllegalStateException("Expected MQTT settings for %s datasource".formatted(dataSourceType));
    }
}
