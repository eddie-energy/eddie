// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.simulation;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.models.datasource.interval.simulation.SimulationDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.api.agnostic.aiida.ObisCode;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class SimulationAdapter extends DataSourceAdapter<SimulationDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationAdapter.class);

    private static final long MIN_POSITIVE_WATT = 200;
    private static final long MAX_POSITIVE_WATT = 1800;
    private static final long AVG_POSITIVE_WATT = 400;
    private static final long MIN_NEGATIVE_WATT = 0;
    private static final long MAX_NEGATIVE_WATT = 1200;
    private static final long AVG_NEGATIVE_WATT = 100;

    private static final Instant SIMULATION_START = Instant.parse("2024-01-01T00:00:00Z");
    private static final BigDecimal WATT_SECONDS_PER_KWH = new BigDecimal(3_600_000L);
    private static final BigDecimal WATT_PER_KW = new BigDecimal(1000L);

    private final Random random;

    private long totalPositiveEnergy = 0;
    private long totalNegativeEnergy = 0;

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

        LOGGER.info(
                "Created new SimulationDataSource that will publish random values every {} seconds",
                dataSource.pollingInterval());
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

        var secondsSinceStart = ChronoUnit.SECONDS.between(SIMULATION_START, Instant.now());
        totalPositiveEnergy = AVG_POSITIVE_WATT * secondsSinceStart;
        totalNegativeEnergy = AVG_NEGATIVE_WATT * secondsSinceStart;

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

    private BigDecimal wattToKiloWatt(long watt) {
        return BigDecimal.valueOf(watt)
                         .divide(WATT_PER_KW, 3, RoundingMode.HALF_UP);
    }

    private BigDecimal wattSecondsToKiloWattHours(long wattSeconds) {
        return BigDecimal.valueOf(wattSeconds)
                         .divide(WATT_SECONDS_PER_KWH, 8, RoundingMode.HALF_UP);
    }

    private void emitRandomAiidaRecords() {
        var positivePower = random.nextLong(MIN_POSITIVE_WATT, MAX_POSITIVE_WATT);
        var negativePower = random.nextLong(MIN_NEGATIVE_WATT, MAX_NEGATIVE_WATT);

        var positiveEnergy = positivePower * dataSource.pollingInterval();
        var negativeEnergy = negativePower * dataSource.pollingInterval();

        totalPositiveEnergy += positiveEnergy;
        totalNegativeEnergy += negativeEnergy;

        var mappings = Map.of(
                ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER, wattToKiloWatt(positivePower),
                ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER, wattToKiloWatt(negativePower),
                ObisCode.POSITIVE_ACTIVE_ENERGY, wattSecondsToKiloWattHours(totalPositiveEnergy),
                ObisCode.NEGATIVE_ACTIVE_ENERGY, wattSecondsToKiloWattHours(totalNegativeEnergy)
        );

        var values = new ArrayList<AiidaRecordValue>();
        mappings.forEach((code, value) -> values.add(
                new AiidaRecordValue(code.toString(),
                                     code,
                                     value.toString(),
                                     code.unitOfMeasurement(),
                                     value.toString(),
                                     code.unitOfMeasurement())));

        emitAiidaRecord(values);
    }
}
