package energy.eddie.core.services;

import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0_82.outbound.ManualTermination;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.cim.v0_82.cmd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class TerminationAdminConsoleConnector implements TerminationConnector, ManualTermination {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationAdminConsoleConnector.class);
    private final Sinks.Many<Pair<String, ConsentMarketDocument>> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

    @Override
    public void terminate(String permissionId, String regionConnectorId) {
        LOGGER.debug("Terminating permission with id {} for region connector with id {}", permissionId, regionConnectorId);
        ConsentMarketDocument cmd = new ConsentMarketDocument()
                .withMRID(permissionId)
                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                .withPermissionList(new ConsentMarketDocument.PermissionList()
                        .withPermissions(
                                new PermissionComplexType()
                                        .withReasonList(
                                                new PermissionComplexType.ReasonList()
                                                        .withReasons(
                                                                new ReasonComplexType()
                                                                        .withCode(ReasonCodeTypeList.CANCELLED_EP)
                                                        )
                                        )
                                        .withMktActivityRecordList(
                                                new PermissionComplexType.MktActivityRecordList()
                                                        .withMktActivityRecords(
                                                                new MktActivityRecordComplexType()
                                                                        .withType(regionConnectorId)
                                                        )
                                        )
                        )
                );
        sink.tryEmitNext(new Pair<>(null, cmd));
    }

    @Override
    public Flux<Pair<String, ConsentMarketDocument>> getTerminationMessages() {
        return sink.asFlux();
    }
}