package energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.cmd.*;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntermediateConsentMarketDocumentTest {

    public static Stream<Arguments> toConsentMarketDocument_mapsSuccessfully() {
        return Stream.of(
                Arguments.of("AT", CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME),
                Arguments.of("DK", CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME),
                Arguments.of("AIIDA", null)
        );
    }

    @Test
    void toConsentMarkDocument_returns() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = today.minusDays(10);
        LocalDate end = today.minusDays(5);

        var dataSourceInformation = mock(DataSourceInformation.class);
        when(dataSourceInformation.countryCode()).thenReturn("AT");
        when(dataSourceInformation.permissionAdministratorId()).thenReturn("paID");
        when(dataSourceInformation.regionConnectorId()).thenReturn("rc");

        var permissionRequest = mock(PermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn("pid", "pid");
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.created()).thenReturn(now);
        when(permissionRequest.dataSourceInformation()).thenReturn(dataSourceInformation);
        when(permissionRequest.start()).thenReturn(start);
        when(permissionRequest.end()).thenReturn(end);
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        IntermediateConsentMarketDocument<PermissionRequest> csm = new IntermediateConsentMarketDocument<>(
                permissionRequest,
                "customerId",
                ignored -> Granularity.PT15M.name(),
                "NAT",
                ZoneOffset.UTC
        );

        // When
        var res = csm.toConsentMarketDocument();

        // Then
        assertNotNull(res);
    }

    @ParameterizedTest
    @MethodSource
    @SuppressWarnings("java:S5961")
        // Too many assertions here, but the CIM requires that many mapping steps.
    void toConsentMarketDocument_mapsSuccessfully(String countryCode, CodingSchemeTypeList codingScheme) {
        // Given
        Clock clock = Clock.fixed(Instant.now(Clock.systemUTC()), ZoneOffset.UTC);
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.minusDays(10);
        LocalDate end = today.minusDays(5);
        var timeInterval = new EsmpTimeInterval(start, end, ZoneOffset.UTC);

        var dataSourceInformation = mock(DataSourceInformation.class);
        when(dataSourceInformation.countryCode()).thenReturn(countryCode);
        when(dataSourceInformation.permissionAdministratorId()).thenReturn("paID");
        when(dataSourceInformation.regionConnectorId()).thenReturn("rc");

        var permissionRequest = mock(PermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn("pid", "pid");
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.created()).thenReturn(now);
        when(permissionRequest.dataSourceInformation()).thenReturn(dataSourceInformation);
        when(permissionRequest.start()).thenReturn(start);
        when(permissionRequest.end()).thenReturn(end);
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        IntermediateConsentMarketDocument<PermissionRequest> csm = new IntermediateConsentMarketDocument<>(
                permissionRequest,
                "customerId",
                ignored -> Granularity.PT15M.name(),
                "NAT",
                ZoneOffset.UTC
        );

        // When
        var res = csm.toConsentMarketDocument(clock);

        // Then
        assertAll(
                () -> assertEquals("pid", res.getMRID()),
                () -> assertEquals("0.82", res.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.PERMISSION_ADMINISTRATION_DOCUMENT, res.getType()),
                () -> assertEquals(new EsmpDateTime(now).toString(), res.getCreatedDateTime()),
                () -> assertEquals("dnid", res.getDescription()),
                () -> assertEquals(RoleTypeList.PARTY_CONNECTED_TO_GRID,
                                   res.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.PERMISSION_ADMINISTRATOR,
                                   res.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.ACCESS_TO_METERED_DATA, res.getProcessProcessType()),
                () -> assertEquals(codingScheme, res.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("customerId", res.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(codingScheme, res.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("paID", res.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(timeInterval.start(), res.getPeriodTimeInterval().getStart()),
                () -> assertEquals(timeInterval.end(), res.getPeriodTimeInterval().getEnd()),
                () -> assertEquals("pid", res.getPermissionList().getPermissions().getFirst().getPermissionMRID()),
                () -> assertEquals(new EsmpDateTime(now).toString(),
                                   res.getPermissionList().getPermissions().getFirst().getCreatedDateTime()),
                () -> assertNull(res.getPermissionList().getPermissions().getFirst().getTransmissionSchedule()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   res.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                      .getCodingScheme()),
                () -> assertEquals("cid",
                                   res.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                      .getValue()),
                () -> assertNull(res.getPermissionList().getPermissions().getFirst().getReasonList()),
                () -> assertNull(res.getPermissionList().getPermissions().getFirst().getTransmissionSchedule()),
                () -> assertNotNull(res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                       .getMktActivityRecords().getFirst().getMRID()),
                () -> assertNotNull(res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                       .getMktActivityRecords().getFirst().getCreatedDateTime()),
                () -> assertEquals("", res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                          .getMktActivityRecords().getFirst().getDescription()),
                () -> assertEquals("rc", res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                            .getMktActivityRecords().getFirst().getType()),
                () -> assertEquals(StatusTypeList.A107,
                                   res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                      .getMktActivityRecords().getFirst().getStatus())
        );
    }
}
