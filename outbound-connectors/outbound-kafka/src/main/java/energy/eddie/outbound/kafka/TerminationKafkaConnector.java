package energy.eddie.outbound.kafka;

import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class TerminationKafkaConnector implements TerminationConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationKafkaConnector.class);
    private final Sinks.Many<Pair<String, PermissionEnvelope>> sink = Sinks.many().multicast().onBackpressureBuffer();


    @Override
    public Flux<Pair<String, PermissionEnvelope>> getTerminationMessages() {
        return sink.asFlux();
    }

    @KafkaListener(
            groupId = "termination-group",
            id = "eddie-termination-listener",
            topics = "fw.${outbound-connector.kafka.eddie-id}.cim_0_82.termination-md",
            containerFactory = "permissionEnvelopeListenerContainerFactory"
    )
    public void process(
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Payload PermissionEnvelope payload
    ) {
        LOGGER.atDebug()
              .addArgument(() -> payload.getPermissionMarketDocument().getMRID())
              .log("Got new PermissionMarketDocument {}");
        sink.tryEmitNext(new Pair<>(key, payload));
    }
}
