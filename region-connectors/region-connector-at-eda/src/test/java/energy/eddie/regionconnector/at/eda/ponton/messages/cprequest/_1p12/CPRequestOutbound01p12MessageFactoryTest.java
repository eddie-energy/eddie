// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cprequest._1p12;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MarshallerConfig.class})
@Import(MarshallerConfig.class)
class CPRequestOutbound01p12MessageFactoryTest {
    @Autowired
    private Jaxb2Marshaller marshaller;

    @Test
    void outboundMetaData_processVersion0430KeepsTransportVersion0300() {
        var factory = new CPRequestOutbound01p12MessageFactory(marshaller);
        var request = new CPRequestCR("AT009000", "point", "retransmission-conversation",
                LocalDate.of(2026, Month.OCTOBER, 24), LocalDate.of(2026, Month.OCTOBER, 26), null,
                new AtConfiguration("EP123456", null, ""));

        var type = factory.outboundMetaData(request).getMessageType();

        assertAll(() -> assertEquals("CR_REQ_PT_03.00", type.getSchemaSet().getValue()),
                  () -> assertEquals("ANFORDERUNG_PT", type.getName().getValue()),
                  () -> assertEquals("03.00", type.getVersion().getValue()));
    }

    @Test
    void cpRequest_autumnClockChange_preservesLocalDayBoundariesAndConversation() {
        var request = new CPRequestCR("AT009000", "point", "retransmission-conversation",
                LocalDate.of(2026, Month.OCTOBER, 25), LocalDate.of(2026, Month.OCTOBER, 26), null,
                new AtConfiguration("EP123456", null, ""));

        var directory = new CPRequest01p12(request).cpRequest().getProcessDirectory();
        var extension = directory.getExtension();
        var from = extension.getDateTimeFrom().toGregorianCalendar().toZonedDateTime();
        var to = extension.getDateTimeTo().toGregorianCalendar().toZonedDateTime();

        assertAll(() -> assertEquals("retransmission-conversation", directory.getConversationId()),
                  () -> assertEquals(25, java.time.Duration.between(from, to).toHours()),
                  () -> assertEquals(request.start(), from.toLocalDate()),
                  () -> assertEquals(request.end(), to.toLocalDate()));
    }

    @Test
    void createOutboundMessage() {
        // given
        var factory = new CPRequestOutbound01p12MessageFactory(marshaller);
        AtConfiguration atConfiguration = new AtConfiguration("RC100007", null, "");
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
    void cpRequest_usesPrefixedConversationId() {
        // given
        var configuration = new AtConfiguration("EP123456", null, "DEV");
        var dateTime = ZonedDateTime.parse("2026-09-01T12:00:00Z");
        var groupingId = new EdaGroupingIdFactory(configuration)
                .create(dateTime);
        var request = new CPRequestCR(
                "dsoid",
                "meteringpoint",
                groupingId,
                LocalDate.now(AT_ZONE_ID).minusWeeks(1),
                LocalDate.now(AT_ZONE_ID),
                null,
                configuration
        );

        // when
        var cpRequest = new CPRequest01p12(request).cpRequest();

        // then
        assertAll(
                () -> assertTrue(cpRequest.getProcessDirectory().getConversationId().startsWith("DEVEP123456T")),
                () -> assertEquals(groupingId, cpRequest.getProcessDirectory().getConversationId()),
                () -> assertEquals(groupingId, cpRequest.getProcessDirectory().getMessageId())
        );
    }

    @Test
    void isActive_on_30_09_2018_returnsFalse() {
        // given
        var factory = new CPRequestOutbound01p12MessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2018, Month.OCTOBER, 1).minusDays(1));

        // then
        assertFalse(active);
    }

    @Test
    void isActive_on_04_10_2026_returnsFalse() {
        // given
        var factory = new CPRequestOutbound01p12MessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2026, Month.OCTOBER, 4));

        // then
        assertFalse(active);
    }

    @Test
    void isActive_on_05_10_2026_returnsTrue() {
        // given
        var factory = new CPRequestOutbound01p12MessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2026, Month.OCTOBER, 5));

        // then
        assertTrue(active);
    }
}
