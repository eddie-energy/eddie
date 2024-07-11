package energy.eddie.regionconnector.shared.cim.v0_82;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.pmd.*;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static energy.eddie.api.CommonInformationModelVersions.V0_82;

public class IntermediatePermissionMarketDocument<T extends PermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediatePermissionMarketDocument.class);
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

    public PermissionEnveloppe toPermissionMarketDocument() {
        return toPermissionMarketDocument(Clock.systemUTC());
    }

    PermissionEnveloppe toPermissionMarketDocument(Clock clock) {
        EsmpDateTime now = EsmpDateTime.now(clock);
        EsmpDateTime created = new EsmpDateTime(permissionRequest.created());
        EsmpTimeInterval interval = new EsmpTimeInterval(permissionRequest.start(), permissionRequest.end(), zoneId);

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
                                .withCodingScheme(
                                        getCodingScheme()
                                )
                                .withValue(customerIdentifier)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(
                                        getCodingScheme()
                                )
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
                                                                                .withDescription("")
                                                                                .withType(permissionRequest.dataSourceInformation()
                                                                                                           .regionConnectorId())
                                                                                .withStatus(getStatusTypeList())
                                                                )
                                                )
                                )
                );
        return new PermissionEnveloppe()
                .withMessageDocumentHeader(metainformation())
                .withPermissionMarketDocument(pmd);
    }

    @Nullable
    private CodingSchemeTypeList getCodingScheme() {
        var cc = permissionRequest.dataSourceInformation().countryCode();
        try {
            return CodingSchemeTypeList.fromValue("N" + cc);
        } catch (IllegalArgumentException e) {
            // prevent exception spamming until GH-638 is implemented
            if (!cc.equalsIgnoreCase("aiida"))
                LOGGER.info("Unknown country code.", e);
            return null;
        }
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
        String permissionRequestStatus = status.toString().toUpperCase(Locale.ROOT);
        for (var statusType : StatusTypeList.values()) {
            if (statusType.value().toUpperCase(Locale.ROOT).equals(permissionRequestStatus)) {
                return statusType;
            }
        }
        throw new IllegalArgumentException("Unknown enum value for StatusTypeList " + permissionRequestStatus);
    }

    private MessageDocumentHeaderComplexType metainformation() {
        var calendar = DatatypeFactory
                .newDefaultInstance()
                .newXMLGregorianCalendar(LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE));
        return new MessageDocumentHeaderComplexType()
                .withCreationDateTime(calendar)
                .withMessageDocumentHeaderMetaInformation(
                        new MessageDocumentHeaderMetaInformationComplexType()
                                .withConnectionid(permissionRequest.connectionId())
                                .withDataNeedid(permissionRequest.dataNeedId())
                                .withDataType("permission-market-document")
                                .withMessageDocumentHeaderRegion(
                                        new MessageDocumentHeaderRegionComplexType()
                                                .withConnector(permissionRequest.dataSourceInformation()
                                                                                .regionConnectorId())
                                                .withCountry(getCodingScheme())
                                )
                );
    }
}
