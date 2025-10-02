package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.modbus.ModbusDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.at.OesterreichsEnergieDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.cim.CimDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.fr.MicroTeleinfoV3DataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.sga.SmartGatewaysDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.shelly.ShellyDataSourceDto;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.models.datasource.modbus.ModbusDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.mqtt.cim.CimDataSource;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyDataSource;
import energy.eddie.aiida.models.datasource.simulation.SimulationDataSource;
import energy.eddie.aiida.models.image.Image;
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
    @JsonProperty
    @Enumerated(EnumType.STRING)
    protected DataSourceIcon icon;
    @Nullable
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", referencedColumnName = "id")
    protected Image image;

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
        this.icon = dto.icon();
    }

    public static DataSource createFromDto(DataSourceDto dto, UUID userId) {
        return switch (dto) {
            case OesterreichsEnergieDataSourceDto parsedDto -> new OesterreichsEnergieDataSource(parsedDto, userId);
            case MicroTeleinfoV3DataSourceDto parsedDto -> new MicroTeleinfoV3DataSource(parsedDto, userId);
            case SinapsiAlfaDataSourceDto parsedDto -> new SinapsiAlfaDataSource(parsedDto, userId);
            case SmartGatewaysDataSourceDto parsedDto -> new SmartGatewaysDataSource(parsedDto, userId);
            case ShellyDataSourceDto parsedDto -> new ShellyDataSource(parsedDto, userId);
            case InboundDataSourceDto parsedDto -> new InboundDataSource(parsedDto, userId);
            case SimulationDataSourceDto parsedDto -> new SimulationDataSource(parsedDto, userId);
            case ModbusDataSourceDto parsedDto -> new ModbusDataSource(parsedDto, userId);
            case CimDataSourceDto parsedDto -> new CimDataSource(parsedDto, userId);
            default -> throw new IllegalArgumentException(
                    "Unsupported dto type: " + dto.getClass() + " / " + dto.dataSourceType()
            );
        };
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

    public DataSourceIcon icon() {
        return icon;
    }

    @Nullable
    public Image image() {
        return image;
    }

    public void setImage(@Nullable Image image) {
        this.image = image;
    }
}
