// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12.rpmd;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v1_12.*;
import energy.eddie.cim.v1_12.rpmd.*;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.CESUJoinRequestDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.regionconnector.shared.cim.IntermediatePermissionMarketDocument;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.cim.v1_12.DocumentType;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static energy.eddie.cim.CommonInformationModelVersions.V1_12;
import static java.util.Map.entry;

public class IntermediateRequestPermissionMarketDocument<T extends PermissionRequest> implements IntermediatePermissionMarketDocument {
    private static final Map<PermissionProcessStatus, StandardStatusTypeList> EDDIE_STATUS_TO_CIM = Map.ofEntries(
            entry(PermissionProcessStatus.CREATED, StandardStatusTypeList.CREATION),
            entry(PermissionProcessStatus.VALIDATED, StandardStatusTypeList.VALIDATED),
            entry(PermissionProcessStatus.MALFORMED, StandardStatusTypeList.NOT_SATISFIED),
            entry(PermissionProcessStatus.UNABLE_TO_SEND, StandardStatusTypeList.NOT_SATISFIED),
            entry(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, StandardStatusTypeList.IN_PROCESS),
            entry(PermissionProcessStatus.REJECTED, StandardStatusTypeList.REJECTED),
            entry(PermissionProcessStatus.TIMED_OUT, StandardStatusTypeList.TIMED_OUT),
            entry(PermissionProcessStatus.INVALID, StandardStatusTypeList.INVALID),
            entry(PermissionProcessStatus.ACCEPTED, StandardStatusTypeList.CONFIRMED),
            entry(PermissionProcessStatus.REVOKED, StandardStatusTypeList.WITHDRAWN),
            entry(PermissionProcessStatus.UNFULFILLABLE, StandardStatusTypeList.NOT_SATISFIED),
            entry(PermissionProcessStatus.FULFILLED, StandardStatusTypeList.CONFIRMED),
            entry(PermissionProcessStatus.TERMINATED, StandardStatusTypeList.DEACTIVATION),
            entry(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION, StandardStatusTypeList.IN_PROCESS),
            entry(PermissionProcessStatus.FAILED_TO_TERMINATE, StandardStatusTypeList.NOT_SATISFIED),
            entry(PermissionProcessStatus.EXTERNALLY_TERMINATED, StandardStatusTypeList.DEACTIVATION)
    );

    private final T permissionRequest;
    private final String customerIdentifier;
    private final TransmissionScheduleProvider<T> transmissionScheduleProvider;
    private final String countryCode;
    private final ZoneId zoneId;
    private final DataNeed dataNeed;
    private final PermissionProcessStatus status;

    public IntermediateRequestPermissionMarketDocument(
            T permissionRequest,
            String customerIdentifier,
            TransmissionScheduleProvider<T> transmissionScheduleProvider,
            String countryCode,
            ZoneId zoneId,
            DataNeed dataNeed,
            PermissionProcessStatus status
    ) {
        this.permissionRequest = permissionRequest;
        this.customerIdentifier = customerIdentifier;
        this.transmissionScheduleProvider = transmissionScheduleProvider;
        this.countryCode = countryCode;
        this.zoneId = zoneId;
        this.dataNeed = dataNeed;
        this.status = status;
    }

    @Override
    public RequestPermissionEnvelope toPermissionMarketDocument() {
        return toPermissionMarketDocument(Clock.systemUTC());
    }

    RequestPermissionEnvelope toPermissionMarketDocument(Clock clock) {
        var now = ZonedDateTime.now(clock);
        EsmpTimeInterval interval = new EsmpTimeInterval(permissionRequest.start(), permissionRequest.end(), zoneId);
        var codingScheme = CimUtils.getCodingSchemeRpmd(permissionRequest.dataSourceInformation().countryCode());

        var eligiblePartyCodingScheme = StandardCodingSchemeTypeList.fromValue(countryCode).value();
        var pmd = new RequestPermissionMarketDocument()
                .withMRID(permissionRequest.permissionId())
                .withRevisionNumber(V1_12.cimify())
                .withType(StandardMessageTypeList.PERMISSION_DOCUMENT.value())
                .withDescription(permissionRequest.dataNeedId())
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.PARTY_CONNECTED_TO_GRID.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.PERMISSION_ADMINISTRATOR.value())
                .withProcessProcessType(getProcessTypeList().value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(codingScheme)
                                .withValue(customerIdentifier)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(eligiblePartyCodingScheme)
                                .withValue(permissionRequest.dataSourceInformation().permissionAdministratorId())
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                )
                .withRequestPermission(
                        new RequestPermission()
                                .withMRID(permissionRequest.permissionId())
                                .withCreatedDateTime(permissionRequest.created())
                                .withTransmissionSchedule(transmissionSchedule())
                                .withTimeSeries()
                                .withReasons()
                                .withAccountingPoints(
                                        new AccountingPoint()
                                                .withMRID(
                                                        new MeasurementPointIDString()
                                                                .withCodingScheme(eligiblePartyCodingScheme)
                                                                .withValue(permissionRequest.connectionId())
                                                )
                                )
                                .withMktActivityRecords(
                                        new MktActivityRecord()
                                                .withMRID(UUID.randomUUID().toString())
                                                .withCreatedDateTime(now)
                                                .withDescription(status.toString())
                                                .withType(permissionRequest.dataSourceInformation()
                                                                           .regionConnectorId())
                                                .withStatus(getStatusTypeList().value())
                                )
                );
        return new RequestPermissionEnvelope()
                .withMessageDocumentHeader(new DocumentHeader(permissionRequest,
                                                              DocumentType.REQUEST_PERMISSION_MARKET_DOCUMENT)
                                                   .permissionMarketDocumentHeader(clock))
                .withMarketDocument(pmd);
    }

    private StandardProcessTypeList getProcessTypeList() {
        return switch (dataNeed) {
            case ValidatedHistoricalDataDataNeed ignored -> StandardProcessTypeList.ACCESS_TO_METERED_DATA;
            case AiidaDataNeed ignored -> StandardProcessTypeList.ACCESS_TO_METERED_DATA;
            case CESUJoinRequestDataNeed ignored -> StandardProcessTypeList.EXCHANGE_OF_MASTER_DATA;
            case AccountingPointDataNeed ignored -> StandardProcessTypeList.EXCHANGE_OF_MASTER_DATA;
            default -> throw new IllegalArgumentException("Unsupported data need type: " + dataNeed);
        };
    }

    @Nullable
    private Duration transmissionSchedule() {
        var duration = transmissionScheduleProvider.findTransmissionScheduleDuration(permissionRequest);
        if (duration == null) {
            return null;
        }
        return DatatypeFactory.newDefaultInstance().newDuration(duration.toMillis());
    }

    private StandardStatusTypeList getStatusTypeList() {
        if (EDDIE_STATUS_TO_CIM.containsKey(status))
            return EDDIE_STATUS_TO_CIM.get(status);
        throw new IllegalArgumentException("Unknown enum value for StatusTypeList " + status);
    }
}
