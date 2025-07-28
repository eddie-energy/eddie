package energy.eddie.outbound.rest.web.cim.v0_82;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocuments;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
import energy.eddie.outbound.rest.persistence.specifications.InsertionTimeSpecification;
import energy.eddie.outbound.rest.persistence.specifications.JsonPathSpecification;
import energy.eddie.outbound.shared.TopicStructure;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController
@RequestMapping(TopicStructure.CIM_0_82_VALUE)
public class CimController {
    public static final String MESSAGE_DOCUMENT_HEADER = "messageDocumentHeader";
    public static final String META_INFORMATION = "messageDocumentHeaderMetaInformation";
    private final CimConnector cimConnector;
    private final ValidatedHistoricalDataMarketDocumentRepository repository;

    public CimController(CimConnector cimConnector, ValidatedHistoricalDataMarketDocumentRepository repository) {
        this.cimConnector = cimConnector;
        this.repository = repository;
    }

    @GetMapping(value = "/validated-historical-data-md", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ValidatedHistoricalDataEnvelope>> validatedHistoricalDataMdSSE() {
        //noinspection UastIncorrectHttpHeaderInspection
        return ResponseEntity.ok()
                             // Tell reverse proxies like Nginx not to buffer the response
                             .header("X-Accel-Buffering", "no")
                             .body(cimConnector.getHistoricalDataMarketDocumentStream());
    }

    @GetMapping(value = "/validated-historical-data-md", produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public ResponseEntity<ValidatedHistoricalDataMarketDocuments> validatedHistoricalDataMd(
            @RequestParam(required = false) Optional<String> permissionId,
            @RequestParam(required = false) Optional<String> connectionId,
            @RequestParam(required = false) Optional<String> dataNeedId,
            @RequestParam(required = false) Optional<String> countryCode,
            @RequestParam(required = false) Optional<String> regionConnectorId,
            @RequestParam(required = false) Optional<ZonedDateTime> from,
            @RequestParam(required = false) Optional<ZonedDateTime> to
    ) {
        var specification = buildQuery(permissionId,
                                       connectionId,
                                       dataNeedId,
                                       countryCode,
                                       regionConnectorId,
                                       from,
                                       to);
        var all = repository.findAll(specification);
        var messages = new ArrayList<ValidatedHistoricalDataEnvelope>();
        for (var model : all) {
            var payload = model.payload();
            messages.add(payload);
        }
        return ResponseEntity.ok()
                             .body(new ValidatedHistoricalDataMarketDocuments(messages));
    }

    private static Specification<ValidatedHistoricalDataMarketDocumentModel> buildQuery(
            Optional<String> permissionId,
            Optional<String> connectionId,
            Optional<String> dataNeedId,
            Optional<String> countryCode,
            Optional<String> regionConnectorId,
            Optional<ZonedDateTime> from,
            Optional<ZonedDateTime> to
    ) {
        var query = List.of(
                permissionId.map(pid -> new JsonPathSpecification<ValidatedHistoricalDataMarketDocumentModel>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "permissionid"),
                        pid
                )),
                connectionId.map(cid -> new JsonPathSpecification<ValidatedHistoricalDataMarketDocumentModel>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "connectionid"),
                        cid
                )),
                dataNeedId.map(did -> new JsonPathSpecification<ValidatedHistoricalDataMarketDocumentModel>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "dataNeedid"),
                        did
                )),
                countryCode.map(cc -> new JsonPathSpecification<ValidatedHistoricalDataMarketDocumentModel>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "messageDocumentHeaderRegion", "country"),
                        "N" + cc.toUpperCase(Locale.ROOT)
                )),
                regionConnectorId.map(rc -> new JsonPathSpecification<ValidatedHistoricalDataMarketDocumentModel>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "messageDocumentHeaderRegion", "connector"),
                        rc
                )),
                from.map(InsertionTimeSpecification::<ValidatedHistoricalDataMarketDocumentModel>insertedAfterEquals),
                to.map(InsertionTimeSpecification::<ValidatedHistoricalDataMarketDocumentModel>insertedBeforeEquals)
        );
        return Specification.allOf(
                query.stream()
                     .filter(Optional::isPresent)
                     .map(spec -> (Specification<ValidatedHistoricalDataMarketDocumentModel>) spec.get())
                     .toList()
        );
    }
}
