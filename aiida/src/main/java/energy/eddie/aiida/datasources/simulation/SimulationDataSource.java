package energy.eddie.aiida.datasources.simulation;

import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static energy.eddie.aiida.utils.ObisCode.*;

public class SimulationDataSource extends AiidaDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationDataSource.class);
    private final Clock clock;
    private final Random random;
    private final List<String> obisCodes;
    private final Duration simulationPeriod;
    @Nullable
    private Disposable periodicFlux;

    public SimulationDataSource(String id, Clock clock, Duration simulationPeriod) {
        this(id, "SimulationDataSource", clock, simulationPeriod);
    }

    /**
     * Creates a new SimulationDataSource with the given name.
     * It will publish an {@link AiidaRecord} with a random value in {@code simulationPeriod} time gaps
     * for these OBIS codes:
     * <li>1.8.0</li>
     * <li>2.8.0</li>
     * <li>1.7.0</li>
     * <li>2.7.0</li>
     *
     * @param name             Display name of this datasource.
     * @param clock            The timestamps of the generated messages will be taken from this clock.
     * @param simulationPeriod Duration to wait until new random records should be created.
     */
    public SimulationDataSource(String id, String name, Clock clock, Duration simulationPeriod) {
        super(id, name);
        this.clock = clock;
        this.simulationPeriod = simulationPeriod;

        random = new Random();
        obisCodes = List.of(POSITIVE_ACTIVE_ENERGY.code(),
                NEGATIVE_ACTIVE_ENERGY.code(),
                POSITIVE_ACTIVE_INSTANTANEOUS_POWER.code(),
                NEGATIVE_ACTIVE_INSTANTANEOUS_POWER.code());

        LOGGER.info("Created new SimulationDataSource that will publish random values every {} seconds for obis codes {}",
                simulationPeriod.toSeconds(), obisCodes);
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
        LOGGER.info("Starting {}", name());

        periodicFlux = Flux.interval(simulationPeriod)
                .subscribeOn(Schedulers.parallel())
                .subscribe(unused -> emitRandomAiidaRecords());

        // use a separate Flux instead of just returning periodicFlux to be able to emit complete signal in close()
        return recordSink.asFlux();
    }

    private void emitRandomAiidaRecords() {
        Instant now = clock.instant();
        for (String code : obisCodes) {
            var randomRecord = AiidaRecordFactory.createRecord(code, now, random.nextInt(2000));
            var result = recordSink.tryEmitNext(randomRecord);

            if (result.isFailure())
                LOGGER.error("Error while publishing {}. Error was: {}", randomRecord, result);
        }
    }

    /**
     * Stops the periodic generation of {@link AiidaRecord}s with random values and emit a complete signal on the
     * Flux that was returned by {@link #start()}.
     */
    @Override
    public void close() {
        LOGGER.info("Closing {}", name());

        if (periodicFlux != null)
            periodicFlux.dispose();

        // ignore if this fails
        recordSink.tryEmitComplete();
    }
}
