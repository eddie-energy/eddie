package energy.eddie.outbound.kafka.v0_91_08;

import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.cim.v0_91_08.retransmission.RTREnveloppe;
import energy.eddie.outbound.shared.Endpoints;
import energy.eddie.outbound.shared.serde.RetransmissionRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class KafkaRetransmissionConnector implements RetransmissionOutboundConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaRetransmissionConnector.class);
    private final Sinks.Many<RetransmissionRequest> sink = Sinks.many()
                                                                .multicast()
                                                                .onBackpressureBuffer();


    @KafkaListener(
            groupId = "retransmission-group",
            id = "eddie-retransmission-listener",
            topics = "${kafka.retransmission.topic:" + Endpoints.V0_91_08.RETRANSMISSIONS + "}",
            containerFactory = "rtrEnvelopeListenerContainerFactory"
    )
    public void process(
            @Payload RTREnveloppe payload
    ) {
        LOGGER.atDebug()
              .addArgument(payload::getMarketDocumentMRID)
              .log("Got new retransmission request {}");
        var retransmissionRequest = new RetransmissionRequestMapper(payload).toRetransmissionRequest();
        sink.tryEmitNext(retransmissionRequest);
    }

    @Override
    public Flux<RetransmissionRequest> retransmissionRequests() {
        return sink.asFlux();
    }

    @Override
    public void setRetransmissionResultStream(Flux<RetransmissionResult> retransmissionResultStream) {
        // No-Op
    }
}
