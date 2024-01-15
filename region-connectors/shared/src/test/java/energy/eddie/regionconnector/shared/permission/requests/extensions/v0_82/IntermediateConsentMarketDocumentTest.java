package energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.cmd.*;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntermediateConsentMarketDocumentTest {

    @Test
    @SuppressWarnings("java:S5961")
        // Too many assertions here, but the CIM requires that many mapping steps.
    void toConsentMarketDocument_mapsSuccessfully() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = now.minusDays(10);
        ZonedDateTime end = now.minusDays(5);

        var dataSourceInformation = mock(DataSourceInformation.class);
        when(dataSourceInformation.countryCode()).thenReturn("AT");
        when(dataSourceInformation.permissionAdministratorId()).thenReturn("paID");
        when(dataSourceInformation.regionConnectorId()).thenReturn("rc");

        var state = mock(PermissionRequestState.class);
        when(state.status()).thenReturn(PermissionProcessStatus.ACCEPTED);

        var permissionRequest = mock(TimeframedPermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn("pid", "pid");
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.created()).thenReturn(now);
        when(permissionRequest.dataSourceInformation()).thenReturn(dataSourceInformation);
        when(permissionRequest.start()).thenReturn(start);
        when(permissionRequest.end()).thenReturn(end);
        when(permissionRequest.state()).thenReturn(state);
        IntermediateConsentMarketDocument<TimeframedPermissionRequest> csm = new IntermediateConsentMarketDocument<>(
                permissionRequest,
                "customerId",
                ignored -> Granularity.PT15M.name(),
                "NAT"
        );

        // When
        var res = csm.toConsentMarketDocument();

        // Then
        assertAll(
                () -> assertEquals("pid", res.getMRID()),
                () -> assertEquals("0.82", res.getRevisionNumber()),
                () -> assertEquals(MessageTypeList.PERMISSION_ADMINISTRATION_DOCUMENT, res.getType()),
                () -> assertEquals(new EsmpDateTime(now).toString(), res.getCreatedDateTime()),
                () -> assertEquals("dnid", res.getDescription()),
                () -> assertEquals(RoleTypeList.PARTY_CONNECTED_TO_GRID, res.getSenderMarketParticipantMarketRoleType()),
                () -> assertEquals(RoleTypeList.PERMISSION_ADMINISTRATOR, res.getReceiverMarketParticipantMarketRoleType()),
                () -> assertEquals(ProcessTypeList.ACCESS_TO_METERED_DATA, res.getProcessProcessType()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, res.getSenderMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("customerId", res.getSenderMarketParticipantMRID().getValue()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, res.getReceiverMarketParticipantMRID().getCodingScheme()),
                () -> assertEquals("paID", res.getReceiverMarketParticipantMRID().getValue()),
                () -> assertEquals(new EsmpDateTime(start).toString(), res.getPeriodTimeInterval().getStart()),
                () -> assertEquals(new EsmpDateTime(end).toString(), res.getPeriodTimeInterval().getEnd()),
                () -> assertEquals("pid", res.getPermissionList().getPermissions().getFirst().getPermissionMRID()),
                () -> assertEquals(new EsmpDateTime(now).toString(), res.getPermissionList().getPermissions().getFirst().getCreatedDateTime()),
                () -> assertNull(res.getPermissionList().getPermissions().getFirst().getTransmissionSchedule()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, res.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID().getCodingScheme()),
                () -> assertEquals("cid", res.getPermissionList().getPermissions().getFirst().getMarketEvaluationPointMRID().getValue()),
                () -> assertNull(res.getPermissionList().getPermissions().getFirst().getReasonList()),
                () -> assertNull(res.getPermissionList().getPermissions().getFirst().getTransmissionSchedule()),
                () -> assertNotNull(res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList().getMktActivityRecords().getFirst().getMRID()),
                () -> assertNotNull(res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList().getMktActivityRecords().getFirst().getCreatedDateTime()),
                () -> assertEquals("", res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList().getMktActivityRecords().getFirst().getDescription()),
                () -> assertEquals("rc", res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList().getMktActivityRecords().getFirst().getType()),
                () -> assertEquals(StatusTypeList.A107, res.getPermissionList().getPermissions().getFirst().getMktActivityRecordList().getMktActivityRecords().getFirst().getStatus())
        );
    }

}