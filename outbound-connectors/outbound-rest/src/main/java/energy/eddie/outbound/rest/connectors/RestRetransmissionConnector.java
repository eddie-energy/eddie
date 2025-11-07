package energy.eddie.outbound.rest.connectors;

import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.shared.utils.RetransmissionRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RestRetransmissionConnector implements RetransmissionOutboundConnector, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestRetransmissionConnector.class);
    private final Sinks.Many<RetransmissionRequest> rrSink = Sinks.many()
                                                                  .multicast()
                                                                  .onBackpressureBuffer();
    private final Map<String, Sinks.One<RetransmissionResult>> pendingRequests = new ConcurrentHashMap<>();

    @Override
    public Flux<RetransmissionRequest> retransmissionRequests() {
        return rrSink.asFlux();
    }

    @Override
    public void setRetransmissionResultStream(Flux<RetransmissionResult> retransmissionResultStream) {
        LOGGER.info("Setting retransmission result stream");
        retransmissionResultStream
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing retransmission result",
                        err))
                .doOnNext(res -> LOGGER.debug("Received retransmission result for permission request {}",
                                              res.permissionId()))
                .subscribe(res -> {
                    var sink = pendingRequests.remove(res.permissionId());
                    if (sink != null) {
                        sink.tryEmitValue(res);
                    }
                });
    }

    public Mono<RetransmissionResult> publish(RTREnvelope envelope) {
        var retransmissionRequest = new RetransmissionRequestMapper(envelope).toRetransmissionRequest();
        var permissionId = retransmissionRequest.permissionId();

        Sinks.One<RetransmissionResult> sink = Sinks.one();
        pendingRequests.put(permissionId, sink);

        rrSink.tryEmitNext(retransmissionRequest);

        return sink.asMono()
                   .doFinally(sig -> pendingRequests.remove(permissionId));
    }


    @Override
    public void close() {
        rrSink.tryEmitComplete();
    }
}
