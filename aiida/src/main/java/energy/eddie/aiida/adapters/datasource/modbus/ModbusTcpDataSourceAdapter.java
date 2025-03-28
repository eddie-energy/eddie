package energy.eddie.aiida.adapters.datasource.modbus;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.models.datasource.modbus.ModbusDataSource;
import energy.eddie.aiida.models.modbus.ModbusDataPoint;
import energy.eddie.aiida.models.modbus.ModbusDevice;
import energy.eddie.aiida.models.modbus.ModbusSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.services.ModbusDeviceService;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;

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
    public ModbusTcpDataSourceAdapter(ModbusDataSource dataSource) {
        super(dataSource);
        this.modbusDevice = ModbusDeviceService.loadConfig(dataSource.getModbusDevice());
        this.pollingInterval = Math.max(dataSource.pollingInterval() * (long) 1000, this.modbusDevice.getIntervals().read().minInterval());

        try {
            String ip = dataSource.getModbusIp();
            if (ip != null) {
                this.modbusTcpClient = new ModbusTcpClient(ip, modbusDevice.getPort(), modbusDevice.getUnitId());
            } else {
                LOGGER.error("Failed to create ModbusClientHelper -> Modbus IP is required and must not be null");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create ModbusClientHelper", e);
        }

        LOGGER.info(
                "Created new ModbusDataSource that will publish modbus values every {} seconds",
                dataSource.pollingInterval());
    }

    // For testing purposes only (package-private)
    void setModbusClientHelper(ModbusTcpClient helper) {
        this.modbusTcpClient = helper;
    }


    @Override
    public Flux<AiidaRecord> start() {
        LOGGER.info("Starting {} with polling interval {}ms", dataSource().name(), this.pollingInterval);


        periodicFlux = Flux.interval(Duration.ofMillis(this.pollingInterval))
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

        for (ModbusSource source : modbusDevice.getSources()) {
            for (ModbusDataPoint dp : source.getDatapoints()) {
                if (!dp.isVirtual() && dp.getAccess().canRead()) {
                    readModbusDataPoint(source, dp, recordValues);
                }
            }
        }

        for (ModbusSource source : modbusDevice.getSources()) {
            for (ModbusDataPoint dp : source.getDatapoints()) {
                if (dp.isVirtual() && dp.getAccess().canRead()) {
                    readVirtualModbusDataPoint(source, dp, recordValues);
                }
            }
        }

        if (!recordValues.isEmpty()) {
            emitAiidaRecord(dataSource.asset(), recordValues);
        }
    }

    private void readModbusDataPoint(ModbusSource source, ModbusDataPoint dp, List<AiidaRecordValue> recordValues) {
        if (modbusTcpClient == null) {
            LOGGER.warn("ModbusClientHelper not initialized");
            return;
        }

        try {
            Optional<Object> read = Optional.empty();

            // read based on register type
            switch (dp.getRegisterType()) {
                case HOLDING:
                    read = modbusTcpClient.readHoldingRegister(dp);
                    break;
                case INPUT:
                    read = modbusTcpClient.readInputRegister(dp);
                    break;
                case COIL:
                    read = modbusTcpClient.readCoil(dp);
                    break;
                case DISCRETE:
                    read = modbusTcpClient.readDiscreteInput(dp);
                    break;
                default:
                    LOGGER.warn("Unknown register type for datapoint {}", dp.getId());
                    break;
            }

            read.ifPresent(value -> {
                // 1. Apply transformations if needed
                Object valueObj = applyTransform(dp, value);

                // 2. Apply translations
                valueObj = applyTranslation(dp, valueObj);

                // 3. Store data to calculate virtual datapoints
                String dpKey = source.getId() + "::" + dp.getId();
                datapointValues.put(dpKey, valueObj);

                // 4. Emit value
                LOGGER.debug("Read value {} for datapoint {}", valueObj, dp.getId());

                recordValues.add(new AiidaRecordValue(
                        dp.getId(),
                        dpKey, // Source Key if applicable
                        value.toString(),
                        UnitOfMeasurement.UNKNOWN, // Unit if applicable
                        valueObj.toString(),
                        UnitOfMeasurement.UNKNOWN
                ));
            });
        } catch (Exception e) {
            LOGGER.warn("Failed to read register for datapoint {}: {}", dp.getId(), e.getMessage());
        }
    }

    private void readVirtualModbusDataPoint(ModbusSource source, ModbusDataPoint dp, List<AiidaRecordValue> recordValues) {
        if (dp.getTransform() == null || dp.getSource() == null || dp.getSource().isEmpty()) {
            return;
        }

        try {
            Map<String, Object> context = new HashMap<>();

            for (String sourceKey : dp.getSource()) {
                String fullKey;

                if (sourceKey.contains("::")) {
                    fullKey = sourceKey;
                } else {
                    fullKey = source.getId() + "::" + sourceKey;
                }

                Object value = datapointValues.getOrDefault(fullKey, 0);
                context.put(getValidContextKey(sourceKey), value);
            }

            String expr = getValidContextKey(dp.getTransform());

            // 1. Apply transformations to get value of virtual datapoint
            Object valueObj = MVEL.eval(expr, context);

            // 2. Apply translations
            valueObj = applyTranslation(dp, valueObj);

            String virtualKey = source.getId() + "::" + dp.getId();
            datapointValues.put(virtualKey, valueObj);

            // 4. Emit value
            LOGGER.debug("Calculated virtual value {} for datapoint {}", valueObj, dp.getId());

            recordValues.add(new AiidaRecordValue(
                    dp.getId(),
                    virtualKey, // Source Key if applicable
                    valueObj.toString(),
                    UnitOfMeasurement.UNKNOWN, // Unit if applicable
                    valueObj.toString(),
                    UnitOfMeasurement.UNKNOWN
            ));

        } catch (Exception e) {
            LOGGER.warn("Failed to calculate virtual value for datapoint {}: {}", dp.getId(), e.getMessage());
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


    private Object applyTranslation(ModbusDataPoint dp, Object rawValue) {
        if (dp.getTranslations() == null || dp.getTranslations().isEmpty()) {
            return rawValue;
        }

        String rawStr = rawValue.toString();
        Map<String, String> translations = dp.getTranslations();

        return translations.getOrDefault(rawStr,
                translations.getOrDefault("default", rawStr));
    }

    private Object applyTransform(ModbusDataPoint dp, Object rawValue) {
        if (dp.getTransform() == null || dp.getTransform().isEmpty()) {
            return rawValue;
        }

        try {
            Map<String, Object> context = new HashMap<>();
            String expr = dp.getTransform().replace("${self}", "self");
            expr = expr.replace("@self", "self");
            context.put("self", rawValue);

            return MVEL.eval(expr, context);
        } catch (Exception e) {
            LOGGER.warn("Failed to apply transform '{}' on datapoint {}: {}", dp.getTransform(), dp.getId(), e.getMessage());
            return rawValue;
        }
    }

}
