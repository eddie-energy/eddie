// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p10;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.*;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests pinning the MapStruct mapping to the values the hand-written
 * {@code ECMPList01p10}/{@code EnergyCommunityMeteringPointData01p10}/{@code MeteringPointTimeData01p10}
 * wrapper records produced.
 */
class ECMPListMapperTest {

    @Test
    void testMapping() {
        // Given
        var factory = DatatypeFactory.newDefaultInstance();
        var dateFrom = factory.newXMLGregorianCalendar(2025, 1, 1, 0, 0, 0, 0, 0);
        var dateTo1 = factory.newXMLGregorianCalendar(2026, 1, 1, 0, 0, 0, 0, 0);
        var dateTo2 = factory.newXMLGregorianCalendar(2027, 1, 1, 0, 0, 0, 0, 0);
        var creationDateTime = factory.newXMLGregorianCalendar(2026, 6, 1, 0, 0, 0, 0, 0);
        var ecmpList = new ECMPList()
                .withMarketParticipantDirectory(
                        new MarketParticipantDirectory()
                                .withRoutingHeader(
                                        new RoutingHeader()
                                                .withSender(new RoutingAddress().withMessageAddress("sender-ec"))
                                                .withReceiver(new RoutingAddress().withMessageAddress("receiver-dso"))
                                                .withDocumentCreationDateTime(creationDateTime)
                                )
                )
                .withProcessDirectory(
                        new ProcessDirectory()
                                .withMessageId("msg-1")
                                .withConversationId("conv-1")
                                .withECID("ec-1")
                                .withMPListData(
                                        new MPListData()
                                                .withMeteringPoint("MP1")
                                                .withMPTimeData(),
                                        new MPListData()
                                                .withMeteringPoint("MP2")
                                                .withMPTimeData(
                                                        new MPTimeData()
                                                                .withDateFrom(dateFrom)
                                                                .withDateTo(dateTo1)
                                                                .withEnergyDirection(EnergyDirection.CONSUMPTION)
                                                                .withECPartFact(new BigDecimal("5")),
                                                        new MPTimeData()
                                                                .withDateTo(dateTo2)
                                                                .withEnergyDirection(EnergyDirection.GENERATION)
                                                                .withECPartFact(new BigDecimal("7"))
                                                )
                                )
                );

        // When
        var res = ECMPListMapper.INSTANCE.toEdaECMPList(ecmpList);

        // Then
        assertEquals("msg-1", res.messageId());
        assertEquals("conv-1", res.conversationId());
        assertEquals("ec-1", res.ecId());
        assertEquals("sender-ec", res.senderMessageAddress());
        assertEquals("receiver-dso", res.receiverMessageAddress());
        assertEquals(ZonedDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneId.of("GMT")), res.documentCreationDateTime());
        assertSame(ecmpList, res.getOriginal());

        assertEquals(2, res.mpListData().size());
        assertEquals("MP1", res.mpListData().get(0).meteringPoint());
        assertEquals(0, res.mpListData().get(0).timeData().size());
        assertEquals("MP2", res.mpListData().get(1).meteringPoint());
        var timeData = res.mpListData().get(1).timeData();
        assertEquals(2, timeData.size());
        assertEquals(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT")), timeData.getFirst().dateFrom());
        assertEquals(energy.eddie.api.agnostic.data.needs.EnergyDirection.CONSUMPTION,
                     timeData.get(0).energyDirection());
        assertEquals(BigInteger.valueOf(5), timeData.get(0).ecPartFact());
        assertNull(timeData.get(1).dateFrom());
        assertEquals(energy.eddie.api.agnostic.data.needs.EnergyDirection.PRODUCTION,
                     timeData.get(1).energyDirection());
        assertEquals(BigInteger.valueOf(7), timeData.get(1).ecPartFact());

        assertEquals(Optional.of(ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)), res.endDate("MP2"));
        assertEquals(Optional.empty(), res.endDate("MP1"));
        assertEquals(Optional.empty(), res.endDate("unknown"));
    }
}
