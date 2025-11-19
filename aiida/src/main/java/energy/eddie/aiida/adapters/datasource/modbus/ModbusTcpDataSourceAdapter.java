package energy.eddie.aiida.adapters.datasource.modbus;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.errors.datasource.modbus.ModbusConnectionException;
import energy.eddie.aiida.models.datasource.interval.modbus.ModbusDataSource;
import energy.eddie.aiida.models.modbus.ModbusDataPoint;
import energy.eddie.aiida.models.modbus.ModbusDevice;
import energy.eddie.aiida.models.modbus.ModbusSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
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
    private final ModbusDevice modbusDevice;
    private final long pollingInterval;
    private final Map<String, Object> datapointValues = new HashMap<>();
    @Nullable
    private Disposable periodicFlux;
    @Nullable
    private ModbusTcpClient modbusTcpClient;

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

        if (!dataSource.enabled()) {
            return;
        }

        var ipAddress = dataSource.ipAddress();

        if (ipAddress == null) {
            LOGGER.error("Failed to create ModbusClientHelper -> Modbus IP is required and must not be null");
            throw new IllegalArgumentException("Modbus IP is required and must not be null");
        }

        try {
            this.modbusTcpClient = new ModbusTcpClient(ipAddress, modbusDevice.port(), modbusDevice.unitId());
        } catch (Exception e) {
            throw new ModbusConnectionException("Failed to create ModbusClientHelper for device: "
                                                + dataSource.deviceId() +
                                                ", IP: "
                                                + dataSource.ipAddress(),
                                                e);
        }

        LOGGER.info("Created new ModbusDataSource that will publish modbus values every {} seconds",
                    dataSource.pollingInterval());
    }

    @Override
    public Flux<AiidaRecord> start() {
        LOGGER.info("Starting {} with polling interval {}ms", dataSource().name(), pollingInterval);
        var now = System.currentTimeMillis();
        var delayUntilNextAlignedInterval = pollingInterval - (now % pollingInterval);


        periodicFlux = Flux.interval(Duration.ofMillis(delayUntilNextAlignedInterval),
                                     Duration.ofMillis(pollingInterval))
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

        if (modbusTcpClient != null) {
            modbusTcpClient.close();
        }

        // ignore if this fails
        recordSink.tryEmitComplete();
    }

    @Override
    public Health health() {
        return Health.up().build();
    }

    private void readModbusValues() {
        var recordValues = new ArrayList<AiidaRecordValue>();

        readAllModbusDataPoints(recordValues);
        readAllVirtualDataPoints(recordValues);

        if (!recordValues.isEmpty()) {
            emitAiidaRecord(dataSource.asset(), recordValues);
        }
    }

    private void readAllModbusDataPoints(List<AiidaRecordValue> recordValues) {
        for (ModbusSource source : modbusDevice.sources()) {
            for (ModbusDataPoint dataPoint : source.dataPoints()) {
                if (!dataPoint.virtual() && dataPoint.access().canRead()) {
                    readModbusDataPoint(source, dataPoint, recordValues);
                }
            }
        }
    }

    private void readAllVirtualDataPoints(List<AiidaRecordValue> recordValues) {
        for (ModbusSource source : modbusDevice.sources()) {
            for (ModbusDataPoint dataPoint : source.dataPoints()) {
                if (dataPoint.virtual() && dataPoint.access().canRead()) {
                    readVirtualModbusDataPoint(source, dataPoint, recordValues);
                }
            }
        }
    }

    private void readModbusDataPoint(
            ModbusSource source,
            ModbusDataPoint dataPoint,
            List<AiidaRecordValue> recordValues
    ) {
        if (modbusTcpClient == null) {
            LOGGER.warn("ModbusClientHelper not initialized");
            return;
        }

        // read based on register type
        var readRegister = switch (dataPoint.registerType()) {
            case HOLDING -> modbusTcpClient.readHoldingRegister(dataPoint);
            case INPUT -> modbusTcpClient.readInputRegister(dataPoint);
            case COIL -> modbusTcpClient.readCoil(dataPoint);
            case DISCRETE -> modbusTcpClient.readDiscreteInput(dataPoint);
            default -> {
                LOGGER.warn("Unknown register type for datapoint {}", dataPoint.id());
                yield Optional.empty();
            }
        };

        readRegister.ifPresent(rawValue -> {
            // 1. Apply transformations if needed
            var transformedValue = dataPoint.applyTransform(rawValue);

            // 2. Apply translations
            var value = dataPoint.applyTranslation(transformedValue);

            // 3. Store data to calculate virtual datapoints
            var rawTag = source.id() + "::" + dataPoint.id();
            datapointValues.put(rawTag, value);

            // 4. Emit value
            var dataTag = dataPoint.obisCode();
            var rawUnitOfMeasurement = dataPoint.unitOfMeasurement();
            var unitOfMeasurement = dataTag.unitOfMeasurement();

            LOGGER.debug("Read value {} for datapoint {}", value, dataPoint.id());
            recordValues.add(new AiidaRecordValue(rawTag,
                                                  dataTag,
                                                  rawValue.toString(),
                                                  rawUnitOfMeasurement,
                                                  value.toString(),
                                                  unitOfMeasurement)
            );
        });
    }

    private void readVirtualModbusDataPoint(
            ModbusSource source,
            ModbusDataPoint dataPoint,
            List<AiidaRecordValue> recordValues
    ) {
        if (!dataPoint.isValidVirtualDatapoint()) {
            LOGGER.warn("Misconfigured virtual datapoint {}", dataPoint.id());
            return;
        }

        try {
            var context = new HashMap<String, Object>();

            for (var dataPointSource : dataPoint.sources()) {
                var fullKey = dataPointSource.contains("::") ? dataPointSource : source.id() + "::" + dataPointSource;
                var value = datapointValues.getOrDefault(fullKey, 0);
                context.put(getValidContextKey(dataPointSource), value);
            }

            var contextKey = getValidContextKey(dataPoint.transform());

            // 1. Apply transformations to get value of virtual datapoint
            var transformedVirtualValue = MVEL.eval(contextKey, context);

            // 2. Apply translations
            transformedVirtualValue = dataPoint.applyTranslation(transformedVirtualValue);

            var rawVirtualTag = source.id() + "::" + dataPoint.id();
            datapointValues.put(rawVirtualTag, transformedVirtualValue);

            // 4. Emit value
            var dataTag = dataPoint.obisCode();
            var rawUnitOfMeasurement = dataPoint.unitOfMeasurement();
            var unitOfMeasurement = dataTag.unitOfMeasurement();

            LOGGER.debug("Calculated virtual value {} for datapoint {}", transformedVirtualValue, dataPoint.id());

            recordValues.add(new AiidaRecordValue(rawVirtualTag,
                                                  dataTag,
                                                  transformedVirtualValue.toString(),
                                                  rawUnitOfMeasurement,
                                                  transformedVirtualValue.toString(),
                                                  unitOfMeasurement)
            );
        } catch (Exception e) {
            LOGGER.warn("Failed to calculate virtual value for datapoint {}: {}", dataPoint.id(), e.getMessage());
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
