package energy.eddie.aiida.adapters.datasource.modbus;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.errors.datasource.modbus.ModbusConnectionException;
import energy.eddie.aiida.models.datasource.interval.modbus.ModbusDataSource;
import energy.eddie.aiida.models.modbus.ModbusDataPoint;
import energy.eddie.aiida.models.modbus.ModbusDevice;
import energy.eddie.aiida.models.modbus.ModbusSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.services.ModbusDeviceService;
import jakarta.annotation.Nullable;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ModbusTcpDataSourceAdapter extends DataSourceAdapter<ModbusDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModbusTcpDataSourceAdapter.class);

    @Nullable
    private Disposable periodicFlux;

    private final ModbusDevice modbusDevice;
    private final long pollingInterval;
    @Nullable
    private ModbusTcpClient modbusTcpClient;

    private final Map<String, Object> datapointValues = new HashMap<>();

    /**
     * Creates a new {@code DataSourceAdapter} with the specified data source entity.
     *
     * @param dataSource The entity of the data source.
     */
    public ModbusTcpDataSourceAdapter(ModbusDataSource dataSource) throws IllegalArgumentException, ModbusConnectionException {
        super(dataSource);
        this.modbusDevice = ModbusDeviceService.loadConfig(dataSource.deviceId());
        this.pollingInterval = Math.max(TimeUnit.SECONDS.toMillis(dataSource.pollingInterval()),
                                        this.modbusDevice.intervals().read().minInterval());

        if (!dataSource.enabled()) return;

        String ip = dataSource.ipAddress();

        if (ip == null) {
            LOGGER.error("Failed to create ModbusClientHelper -> Modbus IP is required and must not be null");
            throw new IllegalArgumentException("Modbus IP is required and must not be null");
        }

        try {
            this.modbusTcpClient = new ModbusTcpClient(ip, modbusDevice.port(), modbusDevice.unitId());
        } catch (Exception e) {
            throw new ModbusConnectionException("Failed to create ModbusClientHelper for device: " + dataSource.deviceId() + ", IP: " + dataSource.ipAddress(),
                                                e);
        }

        LOGGER.info(
                "Created new ModbusDataSource that will publish modbus values every {} seconds",
                dataSource.pollingInterval());
    }

    @Override
    public Flux<AiidaRecord> start() {
        LOGGER.info("Starting {} with polling interval {}ms", dataSource().name(), this.pollingInterval);
        long now = System.currentTimeMillis();
        long delayUntilNextAlignedInterval = this.pollingInterval - (now % this.pollingInterval);


        periodicFlux = Flux.interval(Duration.ofMillis(delayUntilNextAlignedInterval),
                                     Duration.ofMillis(this.pollingInterval))
                           .subscribeOn(Schedulers.parallel())
                           .subscribe(unused -> readModbusValues());

        // use a separate Flux instead of just returning periodicFlux to be able to emit complete signal in close()
        return recordSink.asFlux();
    }

    @Override
    public void close() {
        LOGGER.info("Closing {}", dataSource().name());

        if (periodicFlux != null) {
            periodicFlux.dispose();
        }

        if (this.modbusTcpClient != null) {
            this.modbusTcpClient.close();
        }

        // ignore if this fails
        recordSink.tryEmitComplete();
    }

    @Override
    public Health health() {
        return Health.up().build();
    }

    private void readModbusValues() {
        List<AiidaRecordValue> recordValues = new ArrayList<>();

        readAllModbusDataPoints(recordValues);
        readAllVirtualDataPoints(recordValues);

        if (!recordValues.isEmpty()) {
            emitAiidaRecord(dataSource.asset(), recordValues);
        }
    }

    private void readAllModbusDataPoints(List<AiidaRecordValue> recordValues) {
        for (ModbusSource source : modbusDevice.sources()) {
            for (ModbusDataPoint dp : source.dataPoints()) {
                if (!dp.virtual() && dp.access().canRead()) {
                    readModbusDataPoint(source, dp, recordValues);
                }
            }
        }
    }

    private void readAllVirtualDataPoints(List<AiidaRecordValue> recordValues) {
        for (ModbusSource source : modbusDevice.sources()) {
            for (ModbusDataPoint dp : source.dataPoints()) {
                if (dp.virtual() && dp.access().canRead()) {
                    readVirtualModbusDataPoint(source, dp, recordValues);
                }
            }
        }
    }

    private void readModbusDataPoint(ModbusSource source, ModbusDataPoint dp, List<AiidaRecordValue> recordValues) {
        if (modbusTcpClient == null) {
            LOGGER.warn("ModbusClientHelper not initialized");
            return;
        }

        // read based on register type
        var read = switch (dp.registerType()) {
            case HOLDING -> modbusTcpClient.readHoldingRegister(dp);
            case INPUT -> modbusTcpClient.readInputRegister(dp);
            case COIL -> modbusTcpClient.readCoil(dp);
            case DISCRETE -> modbusTcpClient.readDiscreteInput(dp);
            default -> {
                LOGGER.warn("Unknown register type for datapoint {}", dp.id());
                yield Optional.empty();
            }
        };

        read.ifPresent(value -> {
            // 1. Apply transformations if needed
            Object valueObj = dp.applyTransform(value);

            // 2. Apply translations
            valueObj = dp.applyTranslation(valueObj);

            // 3. Store data to calculate virtual datapoints
            String dpKey = source.id() + "::" + dp.id();
            datapointValues.put(dpKey, valueObj);

            // 4. Emit value
            LOGGER.debug("Read value {} for datapoint {}", valueObj, dp.id());

            recordValues.add(new AiidaRecordValue(
                    dp.id(),
                    dpKey, // Source Key if applicable
                    value.toString(),
                    UnitOfMeasurement.UNKNOWN, // Unit if applicable
                    valueObj.toString(),
                    UnitOfMeasurement.UNKNOWN
            ));
        });
    }

    private void readVirtualModbusDataPoint(
            ModbusSource source,
            ModbusDataPoint dp,
            List<AiidaRecordValue> recordValues
    ) {
        if (!dp.isValidVirtualDatapoint()) {
            LOGGER.warn("Misconfigured virtual datapoint {}", dp.id());
            return;
        }

        try {
            Map<String, Object> context = new HashMap<>();

            for (String sourceKey : dp.source()) {
                String fullKey = sourceKey.contains("::") ? sourceKey : source.id() + "::" + sourceKey;
                Object value = datapointValues.getOrDefault(fullKey, 0);
                context.put(getValidContextKey(sourceKey), value);
            }

            String expr = getValidContextKey(dp.transform());

            // 1. Apply transformations to get value of virtual datapoint
            Object valueObj = MVEL.eval(expr, context);

            // 2. Apply translations
            valueObj = dp.applyTranslation(valueObj);

            String virtualKey = source.id() + "::" + dp.id();
            datapointValues.put(virtualKey, valueObj);

            // 4. Emit value
            LOGGER.debug("Calculated virtual value {} for datapoint {}", valueObj, dp.id());

            recordValues.add(new AiidaRecordValue(
                    dp.id(),
                    virtualKey, // Source Key if applicable
                    valueObj.toString(),
                    UnitOfMeasurement.UNKNOWN, // Unit if applicable
                    valueObj.toString(),
                    UnitOfMeasurement.UNKNOWN
            ));
        } catch (Exception e) {
            LOGGER.warn("Failed to calculate virtual value for datapoint {}: {}", dp.id(), e.getMessage());
        }
    }

    private String getValidContextKey(String key) {
        String expr = key;
        expr = expr.replaceAll("\\$\\{(\\w+)}", "$1");   // Replace ${voltage_l1} → voltage_l1
        expr = expr.replaceAll("@(\\w+)", "$1");
        expr = expr.replace("::", "_");
        expr = expr.replace("-", "_");
        return expr;
    }
}
