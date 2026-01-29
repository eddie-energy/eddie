// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p10;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import(MarshallerConfig.class)
class CMRevoke01p10OutboundMessageFactoryTest {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected Jaxb2Marshaller marshaller;

    @Test
    void createOutboundMessage() {
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

        // when
        var message = new CMRevoke01p10OutboundMessageFactory(marshaller).createOutboundMessage(ccmoRevoke);

        // then
        assertNotNull(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-04-13", "2026-04-14", "2027-04-09", "2028-04-10"})
    void isActive_afterStartDate_ReturnsTrue(LocalDate date) {
        // given
        var factory = new CMRevoke01p10OutboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(date);

        // then
        assertTrue(active);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-04-12"})
    void isActive_whenInactive_ReturnsFalse(LocalDate date) {
        // given
        var factory = new CMRevoke01p10OutboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(date);

        // then
        assertFalse(active);
    }
}
