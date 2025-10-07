package energy.eddie.aiida.dtos.datasource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import energy.eddie.aiida.dtos.datasource.modbus.ModbusDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.at.OesterreichsEnergieDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.cim.CimDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.fr.MicroTeleinfoV3DataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.sga.SmartGatewaysDataSourceDto;
import energy.eddie.aiida.dtos.datasource.mqtt.shelly.ShellyDataSourceDto;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceIcon;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "dataSourceType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OesterreichsEnergieDataSourceDto.class, name = DataSourceType.Identifiers.SMART_METER_ADAPTER),
        @JsonSubTypes.Type(value = CimDataSourceDto.class, name = DataSourceType.Identifiers.CIM_ADAPTER),
        @JsonSubTypes.Type(value = MicroTeleinfoV3DataSourceDto.class, name = DataSourceType.Identifiers.MICRO_TELEINFO),
        @JsonSubTypes.Type(value = InboundDataSourceDto.class, name = DataSourceType.Identifiers.INBOUND),
        @JsonSubTypes.Type(value = SinapsiAlfaDataSourceDto.class, name = DataSourceType.Identifiers.SINAPSI_ALFA),
        @JsonSubTypes.Type(value = SmartGatewaysDataSourceDto.class, name = DataSourceType.Identifiers.SMART_GATEWAYS_ADAPTER),
        @JsonSubTypes.Type(value = ShellyDataSourceDto.class, name = DataSourceType.Identifiers.SHELLY),
        @JsonSubTypes.Type(value = ModbusDataSourceDto.class, name = DataSourceType.Identifiers.MODBUS_TCP),
        @JsonSubTypes.Type(value = SimulationDataSourceDto.class, name = DataSourceType.Identifiers.SIMULATION),
})
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"NullAway.Init"})
public abstract class DataSourceDto {
    @JsonProperty
    protected UUID id;
    @JsonProperty
    protected DataSourceType dataSourceType;
    @JsonProperty
    protected AiidaAsset asset;
    @JsonProperty
    protected String name;
    @JsonProperty
    protected String countryCode;
    @JsonProperty
    protected boolean enabled;
    @JsonProperty
    protected DataSourceIcon icon;

    public UUID id() {
        return id;
    }

    public DataSourceType dataSourceType() {
        return dataSourceType;
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

    public DataSourceIcon icon() {
        return icon;
    }
}
