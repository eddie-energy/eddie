// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p41;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.*;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.dto.EnergyPosition;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests pinning the MapStruct mapping to the values the hand-written
 * {@code EdaConsumptionRecord01p41}/{@code Energy01p41}/{@code EnergyData01p41} wrapper records produced.
 */
class ConsumptionRecordMapperTest {

    @Test
    void testMapping() {
        // Given
        var factory = DatatypeFactory.newDefaultInstance();
        var periodStart1 = factory.newXMLGregorianCalendar(2026, 1, 1, 0, 0, 0, 0, 0);
        var periodEnd1 = factory.newXMLGregorianCalendar(2026, 1, 2, 0, 0, 0, 0, 0);
        var periodEnd4 = factory.newXMLGregorianCalendar(2026, 1, 4, 0, 0, 0, 0, 0);
        var processDate = factory.newXMLGregorianCalendar(2026, 1, 5, 6, 0, 0, 0, 0);
        var creationDateTime = factory.newXMLGregorianCalendar(2026, 1, 5, 7, 0, 0, 0, 0);
        var consumptionRecord = new ConsumptionRecord()
                .withMarketParticipantDirectory(
                        new MarketParticipantDirectory()
                                .withSchemaVersion("01.41")
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
                                .withMeteringPoint("AT1111111111111111111111111111111")
                                .withProcessDate(processDate)
                                .withEnergy(
                                        new Energy()
                                                .withMeteringPeriodStart(periodStart1)
                                                .withMeteringPeriodEnd(periodEnd1)
                                                .withMeteringIntervall(MeteringIntervall.QH)
                                                .withMeteringReason("reason-a")
                                                .withEnergyData(
                                                        new EnergyData()
                                                                .withEP(
                                                                        new at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.EnergyPosition()
                                                                                .withBQ(new BigDecimal("1.5"))
                                                                                .withMM("MM1")
                                                                )
                                                                .withMeterCode("mc-1")
                                                                .withUOM(UOMType.KWH)
                                                ),
                                        new Energy()
                                                .withMeteringPeriodStart(periodEnd1)
                                                .withMeteringPeriodEnd(periodEnd1)
                                                .withMeteringIntervall(MeteringIntervall.H)
                                                .withEnergyData(),
                                        new Energy()
                                                .withMeteringPeriodStart(periodEnd1)
                                                .withMeteringPeriodEnd(periodEnd1)
                                                .withMeteringIntervall(MeteringIntervall.D)
                                                .withEnergyData(),
                                        new Energy()
                                                .withMeteringPeriodStart(periodEnd1)
                                                .withMeteringPeriodEnd(periodEnd4)
                                                .withMeteringIntervall(MeteringIntervall.V)
                                                .withEnergyData()
                                )
                );

        // When
        var res = ConsumptionRecordMapper.INSTANCE.toEdaConsumptionRecord(consumptionRecord);

        // Then
        assertEquals("msg-1", res.messageId());
        assertEquals("conv-1", res.conversationId());
        assertEquals("AT1111111111111111111111111111111", res.meteringPoint());
        assertEquals(LocalDate.of(2026, 1, 1), res.startDate());
        assertEquals(LocalDate.of(2026, 1, 4), res.endDate());
        assertEquals("sender-ec", res.senderMessageAddress());
        assertEquals("receiver-dso", res.receiverMessageAddress());
        assertEquals(ZonedDateTime.of(2026, 1, 5, 7, 0, 0, 0, ZoneOffset.UTC), res.documentCreationDateTime());
        assertEquals(ZonedDateTime.of(2026, 1, 5, 6, 0, 0, 0, ZoneId.of("GMT")), res.processDate());
        assertEquals("01.41", res.schemaVersion());
        assertSame(consumptionRecord, res.originalConsumptionRecord());

        assertEquals(4, res.energy().size());
        assertEquals(Granularity.PT15M, res.energy().get(0).granularity());
        assertEquals(Granularity.PT1H, res.energy().get(1).granularity());
        assertEquals(Granularity.P1D, res.energy().get(2).granularity());
        assertNull(res.energy().get(3).granularity());
        assertEquals(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), res.energy().get(0).meterReadingStart());
        assertEquals(ZonedDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC), res.energy().get(0).meterReadingEnd());
        assertEquals("reason-a", res.energy().getFirst().meteringReason());

        var energyData = res.energy().getFirst().energyData();
        assertEquals(1, energyData.size());
        assertEquals(List.of(new EnergyPosition(new BigDecimal("1.5"), "MM1")),
                     energyData.getFirst().energyPositions());
        assertEquals("mc-1", energyData.getFirst().meterCode());
        assertEquals("KWH", energyData.getFirst().billingUnit());
    }
}
