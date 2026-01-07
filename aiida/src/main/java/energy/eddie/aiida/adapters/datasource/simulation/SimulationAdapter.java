package energy.eddie.aiida.adapters.datasource.simulation;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.api.agnostic.aiida.ObisCode;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulationAdapter extends DataSourceAdapter<SimulationDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationAdapter.class);
    private final Random random;
    private final List<ObisCode> obisCodes;
    @Nullable
    private Disposable periodicFlux;


    /**
     * Creates a new SimulationDataSource with the given name.
     * It will publish an {@link AiidaRecord} with a random value in {@code pollingInterval} time gaps
     * for these OBIS codes:
     * <ul>
     * <li>1-0:1.8.0</li>
     * <li>1-0:2.8.0</li>
     * <li>1-0:1.7.0</li>
     * <li>1-0:2.7.0</li>
     * </ul>
     *
     * @param dataSource The unique identifier (UUID) of this data source.
     */
    public SimulationAdapter(SimulationDataSource dataSource) {
        super(dataSource);

        random = new SecureRandom();
        obisCodes = List.of(ObisCode.POSITIVE_ACTIVE_ENERGY,
                            ObisCode.NEGATIVE_ACTIVE_ENERGY,
                            ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                            ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER);

        LOGGER.info(
                "Created new SimulationDataSource that will publish random values every {} seconds for obis codes {}",
                dataSource.pollingInterval(),
                obisCodes);
    }

    /**
     * Periodically creates an {@link AiidaRecord} for each OBIS code and publishes it on the returned Flux.
     * Calling {@link #close()} will stop the generation.
     * <p>
     * {@link Schedulers#parallel()} is used as Scheduler for the Flux that creates the values.
     * </p>
     *
     * @return Flux of the generated records.
     */
    @Override
    public Flux<AiidaRecord> start() {
        LOGGER.info("Starting {}", dataSource().name());

        periodicFlux = Flux.interval(Duration.ofSeconds(dataSource().pollingInterval()))
                           .subscribeOn(Schedulers.parallel())
                           .subscribe(unused -> emitRandomAiidaRecords());

        // use a separate Flux instead of just returning periodicFlux to be able to emit complete signal in close()
        return recordSink.asFlux();
    }

    /**
     * Stops the periodic generation of {@link AiidaRecord}s with random values and emit a complete signal on the
     * Flux that was returned by {@link #start()}.
     */
    @Override
    public void close() {
        LOGGER.info("Closing {}", dataSource().name());

        if (periodicFlux != null) {
            periodicFlux.dispose();
        }

        // ignore if this fails
        recordSink.tryEmitComplete();
    }

    @Override
    public Health health() {
        return Health.up().build();
    }

    private void emitRandomAiidaRecords() {
        List<AiidaRecordValue> aiidaRecordValues = new ArrayList<>();

        for (ObisCode code : obisCodes) {
            var value = String.valueOf(random.nextInt(2000));

            aiidaRecordValues.add(new AiidaRecordValue(code.toString(),
                                                       code,
                                                       value,
                                                       code.unitOfMeasurement(),
                                                       value,
                                                       code.unitOfMeasurement()));
        }

        emitAiidaRecord(dataSource.asset(), aiidaRecordValues);
    }
}
