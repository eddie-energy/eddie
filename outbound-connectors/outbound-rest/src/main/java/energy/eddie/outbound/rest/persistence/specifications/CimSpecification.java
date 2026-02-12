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
        var header = "MessageDocumentHeader";
        var metaInformation = "MessageDocumentHeader_MetaInformation";

        var query = List.of(
                permissionId.map(pid -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "permissionid"),
                        pid
                )),
                connectionId.map(cid -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "connectionid"),
                        cid
                )),
                dataNeedId.map(did -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "dataNeedid"),
                        did
                )),
                countryCode.map(cc -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "MessageDocumentHeader_Region", "country"),
                        cc.toUpperCase(Locale.ROOT)
                )),
                regionConnectorId.map(rc -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "MessageDocumentHeader_Region", "connector"),
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
        var prefix = "messageDocumentHeader.metaInformation";

        var query = List.of(
                permissionId.map(pid -> new JsonPathSpecification<T>(
                        List.of("%s.permissionId".formatted(prefix)),
                        pid
                )),
                connectionId.map(cid -> new JsonPathSpecification<T>(
                        List.of("%s.connectionId".formatted(prefix)),
                        cid
                )),
                dataNeedId.map(did -> new JsonPathSpecification<T>(
                        List.of("%s.dataNeedId".formatted(prefix)),
                        did
                )),
                countryCode.map(cc -> new JsonPathSpecification<T>(
                        List.of("%s.region.country".formatted(prefix)),
                        cc.toUpperCase(Locale.ROOT)
                )),
                regionConnectorId.map(rc -> new JsonPathSpecification<T>(
                        List.of("%s.region.connector".formatted(prefix)),
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
    public static <T> PredicateSpecification<T> buildQueryForV1_12(
            Optional<String> permissionId,
            Optional<String> connectionId,
            Optional<String> dataNeedId,
            Optional<String> countryCode,
            Optional<String> regionConnectorId,
            Optional<ZonedDateTime> from,
            Optional<ZonedDateTime> to
    ) {
        var header = "MessageDocumentHeader";
        var metaInformation = "MetaInformation";

        var query = List.of(
                permissionId.map(pid -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "requestPermissionId"),
                        pid
                )),
                connectionId.map(cid -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "connectionId"),
                        cid
                )),
                dataNeedId.map(did -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "dataNeedId"),
                        did
                )),
                countryCode.map(cc -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "regionCountry"),
                        cc.toUpperCase(Locale.ROOT)
                )),
                regionConnectorId.map(rc -> new JsonPathSpecification<T>(
                        List.of(header, metaInformation, "regionConnector"),
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
