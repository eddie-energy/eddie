// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence.specifications;

import org.springframework.data.jpa.domain.PredicateSpecification;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CimSpecification {
    private static final String MESSAGE_DOCUMENT_HEADER = "MessageDocumentHeader";
    private static final String META_INFORMATION = "MessageDocumentHeader_MetaInformation";

    private CimSpecification() {
        // No-Op
    }

    // Disable name rule, for readable CIM version
    @SuppressWarnings("java:S100")
    public static <T> PredicateSpecification<T> buildQueryForV0_82(
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
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "MessageDocumentHeader_Region", "country"),
                        cc.toUpperCase(Locale.ROOT)
                )),
                regionConnectorId.map(rc -> new JsonPathSpecification<T>(
                        List.of(MESSAGE_DOCUMENT_HEADER, META_INFORMATION, "MessageDocumentHeader_Region", "connector"),
                        rc
                )),
                from.map(InsertionTimeSpecification::<T>insertedAfterEquals),
                to.map(InsertionTimeSpecification::<T>insertedBeforeEquals)
        );
        return PredicateSpecification.allOf(
                query.stream()
                     .filter(Optional::isPresent)
                     .map(spec -> (PredicateSpecification<T>) spec.get())
                     .toList()
        );
    }

    // Disable name rule, for readable CIM version
    @SuppressWarnings("java:S100")
    public static <T> PredicateSpecification<T> buildQueryForV1_04(
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
                        List.of("messageDocumentHeader.metaInformation.permissionId"),
                        pid
                )),
                connectionId.map(cid -> new JsonPathSpecification<T>(
                        List.of("messageDocumentHeader.metaInformation.connectionId"),
                        cid
                )),
                dataNeedId.map(did -> new JsonPathSpecification<T>(
                        List.of("messageDocumentHeader.metaInformation.dataNeedId"),
                        did
                )),
                countryCode.map(cc -> new JsonPathSpecification<T>(
                        List.of("messageDocumentHeader.metaInformation.region.country"),
                        cc.toUpperCase(Locale.ROOT)
                )),
                regionConnectorId.map(rc -> new JsonPathSpecification<T>(
                        List.of("messageDocumentHeader.metaInformation.region.connector"),
                        rc
                )),
                from.map(InsertionTimeSpecification::<T>insertedAfterEquals),
                to.map(InsertionTimeSpecification::<T>insertedBeforeEquals)
        );
        return PredicateSpecification.allOf(
                query.stream()
                     .filter(Optional::isPresent)
                     .map(spec -> (PredicateSpecification<T>) spec.get())
                     .toList()
        );
    }

    // TODO: Adapt when CIM v1.12 is finalized
    // Disable name rule, for readable CIM version
    @SuppressWarnings("java:S100")
    public static <T> PredicateSpecification<T> buildQueryForV1_12(
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
                        List.of("messageDocumentHeader.metaInformation.permissionId"),
                        pid
                )),
                connectionId.map(cid -> new JsonPathSpecification<T>(
                        List.of("messageDocumentHeader.metaInformation.connectionId"),
                        cid
                )),
                dataNeedId.map(did -> new JsonPathSpecification<T>(
                        List.of("messageDocumentHeader.metaInformation.dataNeedId"),
                        did
                )),
                countryCode.map(cc -> new JsonPathSpecification<T>(
                        List.of("messageDocumentHeader.metaInformation.region.country"),
                        cc.toUpperCase(Locale.ROOT)
                )),
                regionConnectorId.map(rc -> new JsonPathSpecification<T>(
                        List.of("messageDocumentHeader.metaInformation.region.connector"),
                        rc
                )),
                from.map(InsertionTimeSpecification::<T>insertedAfterEquals),
                to.map(InsertionTimeSpecification::<T>insertedBeforeEquals)
        );
        return PredicateSpecification.allOf(
                query.stream()
                     .filter(Optional::isPresent)
                     .map(spec -> (PredicateSpecification<T>) spec.get())
                     .toList()
        );
    }
}
