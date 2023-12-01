package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.ConsumptionRecord;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.annotation.Nullable;

public class ConsumptionRecordHandler {

    @Nullable
    private static ConsumptionRecordHandler singleton;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordHandler.class);

    ConsumptionRecordHandler() {
    }

    private final Sinks.Many<ConsumptionRecord> consumptionRecordStreamSink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return consumptionRecordStreamSink.asFlux();
    }

    void initWebapp(Javalin app) {
        LOGGER.info("Initializing Javalin app");
        app.post(SimulationConnector.basePath() + "/api/consumption-records", ctx -> {
            var consumptionRecord = ctx.bodyAsClass(ConsumptionRecord.class);
            consumptionRecordStreamSink.tryEmitNext(consumptionRecord);
        });
    }

    public static synchronized ConsumptionRecordHandler instance() {
        if (null == singleton) {
            ConsumptionRecordHandler.singleton = new ConsumptionRecordHandler();
        }
        return ConsumptionRecordHandler.singleton;
    }
}
