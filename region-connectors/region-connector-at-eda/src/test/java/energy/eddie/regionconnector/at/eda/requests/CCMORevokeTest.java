package energy.eddie.regionconnector.at.eda.requests;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CCMORevokeTest {
    @Test
    void toCMRevoke_createsExpectedCMRevoke() {
        // Given
        DataSourceInformation dataSourceInformation = mock(DataSourceInformation.class);
        when(dataSourceInformation.permissionAdministratorId()).thenReturn("paId");
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "TestPermissionId",
                "TestConnectionId",
                "TestDataNeedId",
                "TestCmRequestId",
                "TestConversationId",
                "TestDsoId",
                Optional.of("TestMeteringPointId"),
                LocalDate.now(AT_ZONE_ID),
                LocalDate.now(AT_ZONE_ID).plusDays(1),
                PermissionProcessStatus.ACCEPTED,
                Optional.of("TestConsentId")
        );
        String eligiblePartyId = "TestEligiblePartyId";
        CCMORevoke ccmoRevoke = new CCMORevoke(permissionRequest, eligiblePartyId);

        // When
        CMRevoke cmRevoke = ccmoRevoke.toCMRevoke();

        // Then
        assertAll("CMRevoke fields",
                () -> assertEquals(DocumentMode.PROD, cmRevoke.getMarketParticipantDirectory().getDocumentMode()),
                () -> assertFalse(cmRevoke.getMarketParticipantDirectory().isDuplicate()),
                () -> assertEquals("AUFHEBUNG_CCMS", cmRevoke.getMarketParticipantDirectory().getMessageCode()),
                () -> assertEquals("01.00", cmRevoke.getMarketParticipantDirectory().getSchemaVersion()),
                () -> assertEquals("TestMeteringPointId", cmRevoke.getProcessDirectory().getMeteringPoint()),
                () -> assertEquals("TestConsentId", cmRevoke.getProcessDirectory().getConsentId()),
                () -> assertEquals("TestConversationId", cmRevoke.getProcessDirectory().getConversationId()),
                () -> assertNotNull(cmRevoke.getProcessDirectory().getConsentEnd()),
                () -> assertNotNull(cmRevoke.getProcessDirectory().getReason())
        );
    }
}
