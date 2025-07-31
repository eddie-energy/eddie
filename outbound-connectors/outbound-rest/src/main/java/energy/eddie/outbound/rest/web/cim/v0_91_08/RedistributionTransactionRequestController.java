package energy.eddie.outbound.rest.web.cim.v0_91_08;

import energy.eddie.api.agnostic.retransmission.result.*;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.rest.connectors.RestRetransmissionConnector;
import energy.eddie.outbound.shared.TopicStructure;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController
@RequestMapping(TopicStructure.CIM_0_91_08_VALUE)
public class RedistributionTransactionRequestController {
    private final RestRetransmissionConnector retransmissionConnector;

    public RedistributionTransactionRequestController(RestRetransmissionConnector retransmissionConnector) {this.retransmissionConnector = retransmissionConnector;}

    @PostMapping(
            value = "/redistribution-transaction-rd",
            consumes = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE}
    )
    public Mono<ResponseEntity<RetransmissionResult>> redistributionTransactionRd(@RequestBody RTREnvelope rtrEnvelope) {
        return retransmissionConnector.publish(rtrEnvelope)
                                      .map(this::getResponseEntity);
    }

    private ResponseEntity<RetransmissionResult> getResponseEntity(RetransmissionResult res) {
        return switch (res) {
            case DataNotAvailable ignored -> ResponseEntity.noContent().build();
            case Failure ignored -> ResponseEntity.internalServerError().build();
            case NoActivePermission ignored -> ResponseEntity.status(HttpStatus.GONE).build();
            case NoPermissionForTimeFrame ignored -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case NotSupported ignored -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case PermissionRequestNotFound ignored -> ResponseEntity.notFound().build();
            case RetransmissionServiceNotFound ignored -> ResponseEntity.notFound().build();
            case Success ignored -> ResponseEntity.ok().build();
        };
    }
}
