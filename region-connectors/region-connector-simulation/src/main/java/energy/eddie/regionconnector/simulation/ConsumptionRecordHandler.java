package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.ConsumptionRecord;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

public class ConsumptionRecordHandler {

    private static ConsumptionRecordHandler singleton;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordHandler.class);

    ConsumptionRecordHandler() {
        consumptionRecordStream = Flux.create(consumptionRecordStreamSink -> this.consumptionRecordStreamSink = consumptionRecordStreamSink);
    }

    private final Flux<ConsumptionRecord> consumptionRecordStream;
    private FluxSink<ConsumptionRecord> consumptionRecordStreamSink;

    public Flux<ConsumptionRecord> getConsumptionRecordStream() {
        return consumptionRecordStream;
    }

    void initWebapp(Javalin app) {
        LOGGER.info("initializing Javalin app");
        app.post(SimulationConnector.basePath() + "/api/consumption-records", ctx -> {
            var consumptionRecord = ctx.bodyAsClass(ConsumptionRecord.class);
            consumptionRecordStreamSink.next(consumptionRecord);
        });
    }

    synchronized static public ConsumptionRecordHandler instance() {
        if (null == singleton) {
            ConsumptionRecordHandler.singleton = new ConsumptionRecordHandler();
        }
        return ConsumptionRecordHandler.singleton;
    }
}
