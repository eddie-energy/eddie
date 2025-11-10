package energy.eddie.outbound.kafka.v0_91_08;

import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.shared.TopicStructure;
import energy.eddie.outbound.shared.utils.RetransmissionRequestMapper;
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
            topics = "fw.${outbound-connector.kafka.eddie-id}." + TopicStructure.CIM_0_91_08_VALUE + "." + TopicStructure.REDISTRIBUTION_TRANSACTION_RD_VALUE,
            containerFactory = "rtrEnvelopeListenerContainerFactory"
    )
    public void process(
            @Payload RTREnvelope payload
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
