package energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.cim.v0_82.cmd.*;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import static energy.eddie.api.CommonInformationModelVersions.V0_82;

class IntermediateConsentMarketDocument<T extends TimeframedPermissionRequest> {
    private final T permissionRequest;
    private final String customerIdentifier;
    private final TransmissionScheduleProvider<T> transmissionScheduleProvider;
    private final String countryCode;

    public IntermediateConsentMarketDocument(T permissionRequest,
                                             String customerIdentifier,
                                             TransmissionScheduleProvider<T> transmissionScheduleProvider,
                                             String countryCode) {
        this.permissionRequest = permissionRequest;
        this.customerIdentifier = customerIdentifier;
        this.transmissionScheduleProvider = transmissionScheduleProvider;
        this.countryCode = countryCode;
    }

    ConsentMarketDocument toConsentMarketDocument() {
        EsmpDateTime now = EsmpDateTime.now();
        EsmpDateTime created = new EsmpDateTime(permissionRequest.created());
        EsmpDateTime start = new EsmpDateTime(permissionRequest.start());
        String end = permissionRequest.end() == null
                ? null
                : new EsmpDateTime(permissionRequest.end()).toString();

        return new ConsentMarketDocument()
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
                                .withCodingScheme(
                                        CodingSchemeTypeList.fromValue("N" + permissionRequest.dataSourceInformation().countryCode())
                                )
                                .withValue(customerIdentifier)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(
                                        CodingSchemeTypeList.fromValue("N" + permissionRequest.dataSourceInformation().countryCode())
                                )
                                .withValue(permissionRequest.dataSourceInformation().permissionAdministratorId())
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(start.toString())
                                .withEnd(end)
                )
                .withPermissionList(
                        new ConsentMarketDocument.PermissionList()
                                .withPermissions(
                                        new PermissionComplexType()
                                                .withPermissionMRID(permissionRequest.permissionId())
                                                .withCreatedDateTime(created.toString())
                                                .withTransmissionSchedule(transmissionSchedule())
                                                .withMarketEvaluationPointMRID(
                                                        new MeasurementPointIDStringComplexType()
                                                                .withCodingScheme(CodingSchemeTypeList.fromValue(countryCode))
                                                                .withValue(permissionRequest.connectionId())
                                                )
                                                .withMktActivityRecordList(
                                                        new PermissionComplexType.MktActivityRecordList()
                                                                .withMktActivityRecords(
                                                                        new MktActivityRecordComplexType()
                                                                                .withMRID(UUID.randomUUID().toString())
                                                                                .withCreatedDateTime(now.toString())
                                                                                .withDescription("")
                                                                                .withType(permissionRequest.dataSourceInformation().regionConnectorId())
                                                                                .withStatus(getStatusTypeList())
                                                                )
                                                )
                                )
                );
    }

    private StatusTypeList getStatusTypeList() {
        String permissionRequestStatus = permissionRequest.state().status().toString().toUpperCase(Locale.ROOT);
        for (var status : StatusTypeList.values()) {
            if (status.value().toUpperCase(Locale.ROOT).equals(permissionRequestStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown enum value for StatusTypeList " + permissionRequestStatus);
    }

    @Nullable
    private String transmissionSchedule() {
        if (!permissionRequest.start().toLocalDate().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            return null;
        }
        return transmissionScheduleProvider.findTransmissionSchedule(permissionRequest);
    }
}
