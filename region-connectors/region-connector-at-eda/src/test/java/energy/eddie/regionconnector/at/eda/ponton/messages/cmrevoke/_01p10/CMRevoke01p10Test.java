package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p10;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p10.CMRevoke;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;

class CMRevoke01p10Test {
    @Test
    void toCMRevoke_createsExpectedCMRevoke() {
        // Given
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
        CCMORevoke ccmoRevoke = new CCMORevoke(permissionRequest, eligiblePartyId, "TestReason");

        // When
        CMRevoke cmRevoke = new CMRevoke01p10(ccmoRevoke).cmRevoke();

        var expectedConsentEnd = LocalDate.now(AT_ZONE_ID).atStartOfDay(AT_ZONE_ID).toInstant();
        var consentEnd = cmRevoke.getProcessDirectory()
                                 .getConsentEnd()
                                 .toGregorianCalendar()
                                 .toZonedDateTime()
                                 .toInstant();

        // Then
        assertAll("CMRevoke fields",
                  () -> assertEquals(DocumentMode.PROD, cmRevoke.getMarketParticipantDirectory().getDocumentMode()),
                  () -> assertFalse(cmRevoke.getMarketParticipantDirectory().isDuplicate()),
                  () -> assertEquals("AUFHEBUNG_CCMS", cmRevoke.getMarketParticipantDirectory().getMessageCode()),
                  () -> assertEquals("01.10", cmRevoke.getMarketParticipantDirectory().getSchemaVersion()),
                  () -> assertEquals("TestMeteringPointId", cmRevoke.getProcessDirectory().getMeteringPoint()),
                  () -> assertEquals("TestConsentId", cmRevoke.getProcessDirectory().getConsentId()),
                  () -> assertEquals("TestConversationId", cmRevoke.getProcessDirectory().getConversationId()),
                  () -> assertEquals(expectedConsentEnd, consentEnd),
                  () -> assertEquals("TestReason", cmRevoke.getProcessDirectory().getReason())
        );
    }
}
