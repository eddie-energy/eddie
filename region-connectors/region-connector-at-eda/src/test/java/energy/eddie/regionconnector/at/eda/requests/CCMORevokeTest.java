package energy.eddie.regionconnector.at.eda.requests;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CCMORevokeTest {
    @Test
    void toCMRevoke_createsExpectedCMRevoke() {
        // Given
        DataSourceInformation dataSourceInformation = mock(DataSourceInformation.class);
        when(dataSourceInformation.permissionAdministratorId()).thenReturn("paId");
        AtPermissionRequest mockPermissionRequest = mock(AtPermissionRequest.class);
        when(mockPermissionRequest.dataSourceInformation()).thenReturn(dataSourceInformation);
        when(mockPermissionRequest.meteringPointId()).thenReturn(Optional.of("TestMeteringPointId"));
        when(mockPermissionRequest.consentId()).thenReturn(Optional.of("TestConsentId"));
        when(mockPermissionRequest.conversationId()).thenReturn("TestConversationId");
        String eligiblePartyId = "TestEligiblePartyId";
        CCMORevoke ccmoRevoke = new CCMORevoke(mockPermissionRequest, eligiblePartyId);

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
                () -> assertNull(cmRevoke.getProcessDirectory().getConsentEnd()),
                () -> assertNotNull(cmRevoke.getProcessDirectory().getReason())
        );
    }
}