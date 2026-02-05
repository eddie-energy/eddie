// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cprequest._1p12;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MarshallerConfig.class})
@Import(MarshallerConfig.class)
class CPRequestOutbound01p12MessageFactoryTest {
    @Autowired
    private Jaxb2Marshaller marshaller;

    @Test
    void createOutboundMessage() {
        // given
        var factory = new CPRequestOutbound01p12MessageFactory(marshaller);
        AtConfiguration atConfiguration = new AtConfiguration("RC100007", null);
        var request = new CPRequestCR(
                "dsoid",
                "meteringpoint",
                "messageId",
                LocalDate.now(AT_ZONE_ID).minusWeeks(1),
                LocalDate.now(AT_ZONE_ID),
                null,
                atConfiguration
        );
        // when
        var message = factory.createOutboundMessage(request);

        // then
        assertNotNull(message);
    }

    @Test
    void isActive_on_30_09_2018_returnsFalse() {
        // given
        var factory = new CPRequestOutbound01p12MessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2018, 10, 1).minusDays(1));

        // then
        assertFalse(active);
    }

    @Test
    void isActive_on_01_10_2018_returnsTrue() {
        // given
        var factory = new CPRequestOutbound01p12MessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2018, 10, 1));

        // then
        assertTrue(active);
    }
}
