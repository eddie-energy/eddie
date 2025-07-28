package energy.eddie.outbound.rest.persistence.specifications;

import org.springframework.data.jpa.domain.Specification;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CimSpecification {
    private static final String MESSAGE_DOCUMENT_HEADER = "messageDocumentHeader";
    private static final String META_INFORMATION = "messageDocumentHeaderMetaInformation";

    private CimSpecification() {
        // No-Op
    }

    // Disable name rule, for readable CIM version
    @SuppressWarnings("java:S100")
    public static <T> Specification<T> buildQueryForV0_82(
            Optional<String> permissionId,
            Optional<String> connectionId,
            Optional<String> dataNeedId,
            Optional<String> countryCode,
            Optional<String> regionConnectorId,
            Optional<ZonedDateTime> from,
            Optional<ZonedDateTime> to
    ) {
        var query = List.of(
                permissionId.map(pid -> new JsonPathSpecification<T>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "permissionid"),
                        pid
                )),
                connectionId.map(cid -> new JsonPathSpecification<T>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "connectionid"),
                        cid
                )),
                dataNeedId.map(did -> new JsonPathSpecification<T>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "dataNeedid"),
                        did
                )),
                countryCode.map(cc -> new JsonPathSpecification<T>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "messageDocumentHeaderRegion", "country"),
                        cc.toUpperCase(Locale.ROOT)
                )),
                regionConnectorId.map(rc -> new JsonPathSpecification<T>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "messageDocumentHeaderRegion", "connector"),
                        rc
                )),
                from.map(InsertionTimeSpecification::<T>insertedAfterEquals),
                to.map(InsertionTimeSpecification::<T>insertedBeforeEquals)
        );
        return Specification.allOf(
                query.stream()
                     .filter(Optional::isPresent)
                     .map(spec -> (Specification<T>) spec.get())
                     .toList()
        );
    }
}
