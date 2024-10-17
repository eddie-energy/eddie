package energy.eddie.regionconnector.shared.cim.v0_82.pmd;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.pmd.*;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import jakarta.annotation.Nullable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static energy.eddie.api.CommonInformationModelVersions.V0_82;
import static java.util.Map.entry;

public class IntermediatePermissionMarketDocument<T extends PermissionRequest> {
    private static final Map<PermissionProcessStatus, StatusTypeList> EDDIE_STATUS_TO_CIM = Map.ofEntries(
            entry(PermissionProcessStatus.CREATED, StatusTypeList.A14),
            entry(PermissionProcessStatus.VALIDATED, StatusTypeList.Z02),
            entry(PermissionProcessStatus.MALFORMED, StatusTypeList.A33),
            entry(PermissionProcessStatus.UNABLE_TO_SEND, StatusTypeList.A33),
            entry(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, StatusTypeList.A08),
            entry(PermissionProcessStatus.REJECTED, StatusTypeList.A34),
            entry(PermissionProcessStatus.TIMED_OUT, StatusTypeList.Z03),
            entry(PermissionProcessStatus.INVALID, StatusTypeList.Z01),
            entry(PermissionProcessStatus.ACCEPTED, StatusTypeList.A37),
            entry(PermissionProcessStatus.REVOKED, StatusTypeList.A13),
            entry(PermissionProcessStatus.UNFULFILLABLE, StatusTypeList.A33),
            entry(PermissionProcessStatus.FULFILLED, StatusTypeList.A37),
            entry(PermissionProcessStatus.TERMINATED, StatusTypeList.A16),
            entry(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION, StatusTypeList.A08),
            entry(PermissionProcessStatus.FAILED_TO_TERMINATE, StatusTypeList.A33),
            entry(PermissionProcessStatus.EXTERNALLY_TERMINATED, StatusTypeList.A16)
    );
    private final T permissionRequest;
    private final String customerIdentifier;
    private final TransmissionScheduleProvider<T> transmissionScheduleProvider;
    private final String countryCode;
    private final ZoneId zoneId;
    private final PermissionProcessStatus status;

    public IntermediatePermissionMarketDocument(
            T permissionRequest,
            String customerIdentifier,
            TransmissionScheduleProvider<T> transmissionScheduleProvider,
            String countryCode, ZoneId zoneId
    ) {
        this(permissionRequest, permissionRequest.status(), customerIdentifier, transmissionScheduleProvider,
             countryCode, zoneId);
    }

    public IntermediatePermissionMarketDocument(
            T permissionRequest,
            PermissionProcessStatus status,
            String customerIdentifier,
            TransmissionScheduleProvider<T> transmissionScheduleProvider,
            String countryCode, ZoneId zoneId
    ) {
        this.status = status;
        this.permissionRequest = permissionRequest;
        this.customerIdentifier = customerIdentifier;
        this.transmissionScheduleProvider = transmissionScheduleProvider;
        this.countryCode = countryCode;
        this.zoneId = zoneId;
    }

    public PermissionEnvelope toPermissionMarketDocument() {
        return toPermissionMarketDocument(Clock.systemUTC());
    }

    PermissionEnvelope toPermissionMarketDocument(Clock clock) {
        EsmpDateTime now = EsmpDateTime.now(clock);
        EsmpDateTime created = new EsmpDateTime(permissionRequest.created());
        EsmpTimeInterval interval = new EsmpTimeInterval(permissionRequest.start(), permissionRequest.end(), zoneId);
        var codingScheme = CimUtils.getCodingSchemePmd(permissionRequest.dataSourceInformation().countryCode());

        var pmd = new PermissionMarketDocumentComplexType()
                .withMRID(permissionRequest.permissionId())
                .withRevisionNumber(V0_82.version())
                .withType(MessageTypeList.PERMISSION_ADMINISTRATION_DOCUMENT)
                .withCreatedDateTime(now.toString())
                .withDescription(permissionRequest.dataNeedId())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.PARTY_CONNECTED_TO_GRID)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.PERMISSION_ADMINISTRATOR)
                .withProcessProcessType(ProcessTypeList.ACCESS_TO_METERED_DATA)
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(codingScheme)
                                .withValue(customerIdentifier)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(codingScheme)
                                .withValue(permissionRequest.dataSourceInformation().permissionAdministratorId())
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                )
                .withPermissionList(
                        new PermissionMarketDocumentComplexType.PermissionList()
                                .withPermissions(
                                        new PermissionComplexType()
                                                .withPermissionMRID(permissionRequest.permissionId())
                                                .withCreatedDateTime(created.toString())
                                                .withTransmissionSchedule(transmissionSchedule())
                                                .withMarketEvaluationPointMRID(
                                                        new MeasurementPointIDStringComplexType()
                                                                .withCodingScheme(CodingSchemeTypeList.fromValue(
                                                                        countryCode))
                                                                .withValue(permissionRequest.connectionId())
                                                )
                                                .withMktActivityRecordList(
                                                        new PermissionComplexType.MktActivityRecordList()
                                                                .withMktActivityRecords(
                                                                        new MktActivityRecordComplexType()
                                                                                .withMRID(UUID.randomUUID().toString())
                                                                                .withCreatedDateTime(now.toString())
                                                                                .withDescription(status.toString())
                                                                                .withType(permissionRequest.dataSourceInformation()
                                                                                                           .regionConnectorId())
                                                                                .withStatus(getStatusTypeList())
                                                                )
                                                )
                                )
                );
        return new PermissionEnvelope()
                .withMessageDocumentHeader(new DocumentHeader(permissionRequest,
                                                              DocumentType.PERMISSION_MARKET_DOCUMENT).permissionMarketDocumentHeader())
                .withPermissionMarketDocument(pmd);
    }

    @Nullable
    private String transmissionSchedule() {
        // Not all permission requests contain a start date from the beginning, this is just a fact of the process model, where validation happens after the creation of the permission request.
        // Only validated permission requests contain start and end dates.
        if (permissionRequest.start() == null || !permissionRequest.start().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            return null;
        }
        return transmissionScheduleProvider.findTransmissionSchedule(permissionRequest);
    }

    private StatusTypeList getStatusTypeList() {
        if (EDDIE_STATUS_TO_CIM.containsKey(status))
            return EDDIE_STATUS_TO_CIM.get(status);
        throw new IllegalArgumentException("Unknown enum value for StatusTypeList " + status);
    }
}
