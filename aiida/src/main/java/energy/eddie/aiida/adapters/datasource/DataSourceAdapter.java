package energy.eddie.aiida.adapters.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.at.OesterreichsEnergieAdapter;
import energy.eddie.aiida.adapters.datasource.cim.CimAdapter;
import energy.eddie.aiida.adapters.datasource.fr.MicroTeleinfoV3Adapter;
import energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter;
import energy.eddie.aiida.adapters.datasource.it.SinapsiAlfaAdapter;
import energy.eddie.aiida.adapters.datasource.modbus.ModbusTcpDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.sga.SmartGatewaysAdapter;
import energy.eddie.aiida.adapters.datasource.shelly.ShellyAdapter;
import energy.eddie.aiida.adapters.datasource.simulation.SimulationAdapter;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.modbus.ModbusDataSource;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.models.datasource.mqtt.cim.CimDataSource;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyDataSource;
import energy.eddie.aiida.models.datasource.simulation.SimulationDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValidator;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.List;

public abstract class DataSourceAdapter<T extends DataSource> implements AutoCloseable, HealthIndicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceAdapter.class);
    protected final Sinks.Many<AiidaRecord> recordSink;
    protected final Sinks.Many<Health> healthSink;
    protected final T dataSource;

    /**
     * Creates a new {@code DataSourceAdapter} with the specified data source entity.
     *
     * @param dataSource The entity of the data source.
     */
    protected DataSourceAdapter(T dataSource) {
        this.dataSource = dataSource;
        recordSink = Sinks.many().unicast().onBackpressureBuffer();
        healthSink = Sinks.many().unicast().onBackpressureBuffer();
    }

    /**
     * Factory method for {@code DataSourceAdapter} creation.
     *
     * @param dataSource   The entity of the data source.
     * @param objectMapper The object mapper to use for deserialization.
     * @return The created {@code DataSourceAdapter}.
     */
    public static DataSourceAdapter<? extends DataSource> create(
            DataSource dataSource,
            ObjectMapper objectMapper,
            MqttConfiguration mqttConfiguration
    ) {
        return switch (dataSource.dataSourceType()) {
            case SMART_METER_ADAPTER -> new OesterreichsEnergieAdapter((OesterreichsEnergieDataSource) dataSource,
                                                                       objectMapper,
                                                                       mqttConfiguration);
            case MICRO_TELEINFO ->
                    new MicroTeleinfoV3Adapter((MicroTeleinfoV3DataSource) dataSource, objectMapper, mqttConfiguration);
            case SINAPSI_ALFA ->
                    new SinapsiAlfaAdapter((SinapsiAlfaDataSource) dataSource, objectMapper, mqttConfiguration);
            case SMART_GATEWAYS_ADAPTER ->
                    new SmartGatewaysAdapter((SmartGatewaysDataSource) dataSource, mqttConfiguration);
            case SHELLY -> new ShellyAdapter((ShellyDataSource) dataSource, objectMapper, mqttConfiguration);
            case INBOUND -> new InboundAdapter((InboundDataSource) dataSource, mqttConfiguration);
            case SIMULATION -> new SimulationAdapter((SimulationDataSource) dataSource);
            case MODBUS -> new ModbusTcpDataSourceAdapter((ModbusDataSource) dataSource);
            case CIM_ADAPTER -> new CimAdapter((CimDataSource) dataSource, objectMapper, mqttConfiguration);
        };
    }

    /**
     * Starts this datasource and all records will be published on the returned Flux.
     *
     * @return Flux on which all data from this datasource will be published.
     */
    public abstract Flux<AiidaRecord> start();

    /**
     * Emits a new {@code AiidaRecord} with specified aiida record values
     *
     * @param aiidaRecordValues Values for the new aiida record
     */
    public synchronized void emitAiidaRecord(AiidaAsset asset, List<AiidaRecordValue> aiidaRecordValues) {
        Instant timestamp = Instant.now();

        var aiidaRecord = new AiidaRecord(timestamp, asset, dataSource.userId(), dataSource.id(), aiidaRecordValues);
        var invalidTags = AiidaRecordValidator.checkInvalidDataTags(aiidaRecord);

        if (!invalidTags.isEmpty()) {
            LOGGER.trace("Found unknown OBIS-CODES from {}: {}", asset, invalidTags);
        }

        var result = recordSink.tryEmitNext(aiidaRecord);

        if (result.isFailure()) {
            LOGGER.warn("Error while emitting new AiidaRecord {}. Error was {}", aiidaRecord, result);
        }
    }

    /**
     * Closes any open connections and frees resources used by this datasource.
     * Also emits a complete signal on the Flux returned by {@link #start()}.
     */
    @Override
    public abstract void close();

    public T dataSource() {
        return dataSource;
    }
}
