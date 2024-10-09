package energy.eddie.regionconnector.shared.cim.v0_82.pmd;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.pmd.*;
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

class IntermediatePermissionMarketDocumentTest {

    public static Stream<Arguments> toPermissionMarketDocument_mapsSuccessfully() {
        return Stream.of(
                Arguments.of("AT", CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME),
                Arguments.of("DK", CodingSchemeTypeList.DENMARK_NATIONAL_CODING_SCHEME),
                Arguments.of("AIIDA", null)
        );
    }

    @Test
    void toPermissionMarkDocument_returns() {
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
        IntermediatePermissionMarketDocument<PermissionRequest> csm = new IntermediatePermissionMarketDocument<>(
                permissionRequest,
                "customerId",
                ignored -> Granularity.PT15M.name(),
                "NAT",
                ZoneOffset.UTC
        );

        // When
        var res = csm.toPermissionMarketDocument();

        // Then
        assertNotNull(res);
    }

    @ParameterizedTest
    @MethodSource
    @SuppressWarnings("java:S5961")
        // Too many assertions here, but the CIM requires that many mapping steps.
    void toPermissionMarketDocument_mapsSuccessfully(String countryCode, CodingSchemeTypeList codingScheme) {
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
        IntermediatePermissionMarketDocument<PermissionRequest> csm = new IntermediatePermissionMarketDocument<>(
                permissionRequest,
                "customerId",
                ignored -> Granularity.PT15M.name(),
                "NAT",
                ZoneOffset.UTC
        );

        // When
        var res = csm.toPermissionMarketDocument(clock);

        // Then
        var pmd = res.getPermissionMarketDocument();
        assertAll(
                () -> assertEquals("pid", pmd.getMRID()),
                () -> assertEquals("0.82", pmd.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.PERMISSION_ADMINISTRATION_DOCUMENT, pmd.getType()),
                () -> assertEquals(new EsmpDateTime(now).toString(), pmd.getCreatedDateTime()),
                () -> assertEquals("dnid", pmd.getDescription()),
                () -> assertEquals(RoleTypeList.PARTY_CONNECTED_TO_GRID,
                                   pmd.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.PERMISSION_ADMINISTRATOR,
                                   pmd.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.ACCESS_TO_METERED_DATA, pmd.getProcessProcessType()),
                () -> assertEquals(codingScheme, pmd.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("customerId", pmd.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(codingScheme, pmd.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("paID", pmd.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(timeInterval.start(), pmd.getPeriodTimeInterval().getStart()),
                () -> assertEquals(timeInterval.end(), pmd.getPeriodTimeInterval().getEnd()),
                () -> assertEquals("pid", pmd.getPermissionList().getPermissions().getFirst().getPermissionMRID()),
                () -> assertEquals(new EsmpDateTime(now).toString(),
                                   pmd.getPermissionList().getPermissions().getFirst().getCreatedDateTime()),
                () -> assertNull(pmd.getPermissionList().getPermissions().getFirst().getTransmissionSchedule()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                   pmd.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                      .getCodingScheme()),
                () -> assertEquals("cid",
                                   pmd.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID()
                                      .getValue()),
                () -> assertNull(pmd.getPermissionList().getPermissions().getFirst().getReasonList()),
                () -> assertNull(pmd.getPermissionList().getPermissions().getFirst().getTransmissionSchedule()),
                () -> assertNotNull(pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                       .getMktActivityRecords().getFirst().getMRID()),
                () -> assertNotNull(pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                       .getMktActivityRecords().getFirst().getCreatedDateTime()),
                () -> assertEquals("ACCEPTED",
                                   pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                      .getMktActivityRecords().getFirst().getDescription()),
                () -> assertEquals("rc", pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                            .getMktActivityRecords().getFirst().getType()),
                () -> assertEquals(StatusTypeList.A37,
                                   pmd.getPermissionList().getPermissions().getFirst().getMktActivityRecordList()
                                      .getMktActivityRecords().getFirst().getStatus())
        );
    }
}
