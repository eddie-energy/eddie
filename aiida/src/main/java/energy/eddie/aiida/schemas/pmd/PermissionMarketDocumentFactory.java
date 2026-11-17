// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas.pmd;

import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v1_12.StandardMessageTypeList;
import energy.eddie.cim.v1_12.StandardProcessTypeList;
import energy.eddie.cim.v1_12.StandardRoleTypeList;
import energy.eddie.cim.v1_12.StandardStatusTypeList;
import energy.eddie.cim.v1_12.rpmd.*;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static energy.eddie.cim.CommonInformationModelVersions.V1_12;

/**
 * Creates the CIM v1.12 permission market documents AIIDA sends to the EDDIE framework when the status of a permission
 * changes on the AIIDA side. Follows the conventions of the framework's own permission market documents: the CIM
 * status code is in {@code MktActivityRecord.status}, the exact EDDIE status in {@code MktActivityRecord.description}.
 */
@Component
public class PermissionMarketDocumentFactory {
    static final String REGION_CONNECTOR = "aiida";
    static final String DOCUMENT_TYPE = "request-permission-market-document";
    private static final DateTimeFormatter ESMP_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm'Z'")
            .withZone(ZoneOffset.UTC);

    private final ApplicationInformationService applicationInformationService;
    private final Clock clock;

    public PermissionMarketDocumentFactory(ApplicationInformationService applicationInformationService, Clock clock) {
        this.applicationInformationService = applicationInformationService;
        this.clock = clock;
    }

    /**
     * Creates the permission market document announcing the passed status of the passed permission.
     *
     * @param status One of {@link PermissionProcessStatus#REVOKED}, {@link PermissionProcessStatus#EXTERNALLY_TERMINATED}
     *               or {@link PermissionProcessStatus#FULFILLED}.
     * @throws IllegalArgumentException If AIIDA does not send the passed status to the EDDIE framework.
     */
    public RequestPermissionEnvelope create(Permission permission, PermissionProcessStatus status) {
        var cimStatus = toCimStatus(status);
        var now = ZonedDateTime.now(clock);
        var permissionId = permission.id().toString();
        var dataNeed = permission.dataNeed();
        var dataNeedId = dataNeed == null ? null : dataNeed.dataNeedId().toString();
        var finalCustomerId = applicationInformationService.applicationInformation().aiidaId().toString();

        var marketDocument = new RequestPermissionMarketDocument()
                .withMRID(permissionId)
                .withRevisionNumber(V1_12.cimify())
                .withType(StandardMessageTypeList.PERMISSION_DOCUMENT.value())
                .withDescription(dataNeedId)
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.PARTY_CONNECTED_TO_GRID.value())
                .withSenderMarketParticipantMRID(new PartyIDString().withValue(finalCustomerId))
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.PERMISSION_ADMINISTRATOR.value())
                .withProcessProcessType(StandardProcessTypeList.ACCESS_TO_METERED_DATA.value())
                .withPeriodTimeInterval(new ESMPDateTimeInterval()
                                                .withStart(format(permission.startTime()))
                                                .withEnd(format(permission.expirationTime())))
                .withRequestPermission(
                        new RequestPermission()
                                .withMRID(permissionId)
                                .withCreatedDateTime(toZonedDateTime(permission.grantTime()))
                                .withAccountingPoints(
                                        new AccountingPoint()
                                                .withMRID(new MeasurementPointIDString()
                                                                  .withValue(permission.connectionId()))
                                )
                                .withMktActivityRecords(
                                        new MktActivityRecord()
                                                .withMRID(UUID.randomUUID().toString())
                                                .withCreatedDateTime(now)
                                                .withDescription(status.toString())
                                                .withType(REGION_CONNECTOR)
                                                .withStatus(cimStatus.value())
                                )
                );

        var header = new MessageDocumentHeader()
                .withCreationDateTime(now)
                .withMetaInformation(new MetaInformation()
                                             .withConnectionId(permission.connectionId())
                                             .withRequestPermissionId(permissionId)
                                             .withDataNeedId(dataNeedId)
                                             .withDocumentType(DOCUMENT_TYPE)
                                             .withFinalCustomerId(finalCustomerId)
                                             .withRegionConnector(REGION_CONNECTOR));

        return new RequestPermissionEnvelope()
                .withMessageDocumentHeader(header)
                .withMarketDocument(marketDocument);
    }

    private static StandardStatusTypeList toCimStatus(PermissionProcessStatus status) {
        return switch (status) {
            case REVOKED -> StandardStatusTypeList.WITHDRAWN;
            case EXTERNALLY_TERMINATED -> StandardStatusTypeList.DEACTIVATION;
            case FULFILLED -> StandardStatusTypeList.CONFIRMED;
            default -> throw new IllegalArgumentException("AIIDA does not send permission status " + status);
        };
    }

    @Nullable
    private static String format(@Nullable Instant instant) {
        return instant == null ? null : ESMP_DATE_TIME_FORMATTER.format(instant);
    }

    @Nullable
    private static ZonedDateTime toZonedDateTime(@Nullable Instant instant) {
        return instant == null ? null : instant.atZone(ZoneOffset.UTC);
    }
}
