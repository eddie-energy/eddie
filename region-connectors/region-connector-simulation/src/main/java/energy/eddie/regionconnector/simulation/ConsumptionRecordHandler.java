package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.ConsumptionRecord;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.annotation.Nullable;

public class ConsumptionRecordHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordHandler.class);
    @Nullable
    private static ConsumptionRecordHandler singleton;
    private final Sinks.Many<ConsumptionRecord> consumptionRecordStreamSink = Sinks.many().multicast().onBackpressureBuffer();

    ConsumptionRecordHandler() {
    }

    public static synchronized ConsumptionRecordHandler instance() {
        if (null == singleton) {
            ConsumptionRecordHandler.singleton = new ConsumptionRecordHandler();
        }
        return ConsumptionRecordHandler.singleton;
    }

    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return consumptionRecordStreamSink.asFlux();
    }

    void initWebapp(Javalin app) {
        LOGGER.info("Initializing Javalin app");
        String basePath = SimulationConnectorMetadata.getInstance().id();
        app.post(basePath + "/api/consumption-records", ctx -> {
            var consumptionRecord = ctx.bodyAsClass(ConsumptionRecord.class);
            consumptionRecordStreamSink.tryEmitNext(consumptionRecord);
        });
    }
}