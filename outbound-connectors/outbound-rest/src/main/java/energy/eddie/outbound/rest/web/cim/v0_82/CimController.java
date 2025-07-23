package energy.eddie.outbound.rest.web.cim.v0_82;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import energy.eddie.outbound.shared.TopicStructure;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(TopicStructure.CIM_0_82_VALUE)
public class CimController {
    private final CimConnector cimConnector;

    public CimController(CimConnector cimConnector) {this.cimConnector = cimConnector;}

    @GetMapping(value = "validated-historical-data-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ValidatedHistoricalDataEnvelope>> validatedHistoricalDataMdSSE() {
        //noinspection UastIncorrectHttpHeaderInspection
        return ResponseEntity.ok()
                             // Tell reverse proxies like Nginx not to buffer the response
                             .header("X-Accel-Buffering", "no")
                             .body(cimConnector.getHistoricalDataMarketDocumentStream());
    }
}
