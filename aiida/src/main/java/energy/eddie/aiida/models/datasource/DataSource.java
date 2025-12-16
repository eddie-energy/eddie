package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.modbus.ModbusDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.at.OesterreichsEnergieDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.cim.CimDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.fr.MicroTeleinfoV3DataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.sga.SmartGatewaysDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.shelly.ShellyDataSourceDto;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.models.datasource.interval.modbus.ModbusDataSource;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.mqtt.cim.CimDataSource;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyDataSource;
import energy.eddie.aiida.models.image.Image;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "data_source")
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class DataSource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    @JsonProperty
    protected UUID id;

    @Column(name = "user_id", nullable = false)
    @JsonIgnore
    protected UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "The type of asset this data source is categorized as.")
    @JsonProperty
    protected AiidaAsset asset;

    @Column(name = "country_code", nullable = false, length = 2)
    @Schema(description = "The country code of the country the data source is located at.")
    @JsonProperty
    protected String countryCode;

    @Column(nullable = false)
    @Schema(description = "The state of the data source, whether it is enabled or not.")
    @JsonProperty
    protected boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "The icon associated with the data source.")
    @JsonProperty
    protected DataSourceIcon icon;

    @Nullable
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", referencedColumnName = "id")
    @JsonIgnore
    protected Image image;

    @Column(nullable = false)
    @Schema(description = "The name of the data source.")
    @JsonProperty
    protected String name;

    @OneToMany(mappedBy = "dataSource", targetEntity = Permission.class, fetch = FetchType.LAZY)
    @JsonIgnore
    protected List<Permission> permissions;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", insertable = false, updatable = false)
    @Schema(description = "The type of the data source.")
    @JsonProperty
    protected DataSourceType type;

    @SuppressWarnings("NullAway")
    protected DataSource() {}

    protected DataSource(DataSourceDto dto, UUID userId) {
        applyDto(dto, userId);
    }

    protected DataSource(DataSource dataSource) {
        id = dataSource.id;
        userId = dataSource.userId;
        asset = dataSource.asset;
        countryCode = dataSource.countryCode;
        enabled = dataSource.enabled;
        icon = dataSource.icon;
        image = dataSource.image;
        name = dataSource.name;
        permissions = dataSource.permissions;
        type = dataSource.type;
    }

    public static DataSource createFromDto(DataSourceDto dto, UUID userId) {
        return switch (dto) {
            case OesterreichsEnergieDataSourceDto parsedDto -> new OesterreichsEnergieDataSource(parsedDto, userId);
            case MicroTeleinfoV3DataSourceDto parsedDto -> new MicroTeleinfoV3DataSource(parsedDto, userId);
            case SinapsiAlfaDataSourceDto parsedDto -> new SinapsiAlfaDataSource(parsedDto, userId);
            case SmartGatewaysDataSourceDto parsedDto -> new SmartGatewaysDataSource(parsedDto, userId);
            case ShellyDataSourceDto parsedDto -> new ShellyDataSource(parsedDto, userId);
            case SimulationDataSourceDto parsedDto -> new SimulationDataSource(parsedDto, userId);
            case ModbusDataSourceDto parsedDto -> new ModbusDataSource(parsedDto, userId);
            case CimDataSourceDto parsedDto -> new CimDataSource(parsedDto, userId);
            default -> throw new IllegalArgumentException(
                    "Unsupported dto type: " + dto.getClass() + " / " + dto.type()
            );
        };
    }

    public void update(DataSourceDto dto) {
        applyDto(dto, this.userId);
    }

    public DataSourceType type() {
        return type;
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

    public List<Permission> permissions() {
        return permissions;
    }

    private void applyDto(DataSourceDto dto, UUID userId) {
        this.id = dto.id();
        this.userId = userId;
        this.asset = dto.asset();
        this.name = dto.name();
        this.enabled = dto.enabled();
        this.type = dto.type();
        this.countryCode = dto.countryCode();
        this.icon = dto.icon();
        this.permissions = List.of();
    }

    @PreRemove
    private void preRemove() {
        for (var permission : permissions) {
            permission.setDataSource(null);
        }
    }
}
