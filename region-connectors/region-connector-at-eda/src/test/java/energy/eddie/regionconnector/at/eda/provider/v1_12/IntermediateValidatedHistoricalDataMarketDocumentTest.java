// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.ConsumptionRecord;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_12.StandardDirectionTypeList;
import energy.eddie.cim.v1_12.StandardQualityTypeList;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p41.EdaConsumptionRecord01p41;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "epID"
    );

    @ParameterizedTest
    @MethodSource("meterCodeAndMeteringModeSource")

    @SuppressWarnings("java:S5961")
    void toVhd_returnsEnvelopes(
            String meterCode,
            StandardDirectionTypeList expectedDirection,
            String meteringMode,
            StandardQualityTypeList expectedQuality
    ) {
        var pr = new SimplePermissionRequest("pid", "cid", "dnid");
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);
        var now = ZonedDateTime.now(AT_ZONE_ID);
        var simpleRecord = new SimpleEdaConsumptionRecord()
                .setMessageId("messageId")
                .setConversationId("conversationId")
                .setMeteringPoint(meterCode)
                .setStartDate(start)
                .setEndDate(end)
                .setSenderMessageAddress("eda")
                .setReceiverMessageAddress("eddie")
                .setDocumentCreationDateTime(now)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(Granularity.P1D)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setEnergyPositions(List.of(
                                                        new EnergyPosition(BigDecimal.ONE, meteringMode)
                                                ))
                                                .setMeterCode(meterCode)
                                                .setBillingUnit("KWH")
                                ))
                                .setMeterReadingStart(start.atStartOfDay(AT_ZONE_ID))
                                .setMeterReadingEnd(endOfDay(end, AT_ZONE_ID))
                                .setMeteringReason("reason")
                ))
                .setSchemaVersion("version")
                .setProcessDate(now);
        var consumptionRecord = new IdentifiableConsumptionRecord(simpleRecord, List.of(pr), start, end);
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord);

        var res = doc.toVhd();

        assertEquals(1, res.size());
        var series = res.getFirst().getVHDMarketDocument().getTimeSeries().getFirst();
        assertEquals(expectedDirection.value(), series.getFlowDirectionDirection());
        assertEquals(expectedQuality.value(), series.getSeriesPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuality());
        assertHistoricalDataRoundTrips(res.getFirst());
    }

    @Test
    void toVhd_producesValidXml() {
        var pr = new SimplePermissionRequest("pid", "cid", "dnid");
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 2);
        var created = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, AT_ZONE_ID);
        var simpleRecord = new SimpleEdaConsumptionRecord()
                .setMessageId("messageId")
                .setConversationId("conversationId")
                .setMeteringPoint("1-1:1.9.0 P.01")
                .setStartDate(start)
                .setEndDate(end)
                .setSenderMessageAddress("eda")
                .setReceiverMessageAddress("eddie")
                .setDocumentCreationDateTime(created)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(Granularity.P1D)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setEnergyPositions(List.of(
                                                        new EnergyPosition(BigDecimal.ONE, "L1")
                                                ))
                                                .setMeterCode("1-1:1.9.0 P.01")
                                                .setBillingUnit("KWH")
                                ))
                                .setMeterReadingStart(start.atStartOfDay(AT_ZONE_ID))
                                .setMeterReadingEnd(endOfDay(end, AT_ZONE_ID))
                                .setMeteringReason("reason")
                ))
                .setSchemaVersion("version")
                .setProcessDate(created);
        var consumptionRecord = new IdentifiableConsumptionRecord(simpleRecord, List.of(pr), start, end);
        var doc = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, consumptionRecord);

        var res = doc.toVhd();

        assertHistoricalDataRoundTrips(res.getFirst());
    }

    @Test
    void givenConsumptionRecord_whenThVhd_returnsValidVhd() throws JAXBException {
        var payload = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns0:ConsumptionRecord xmlns:ns0="http://www.ebutilities.at/schemata/customerprocesses/consumptionrecord/01p41"
                                       xmlns:ns1="http://www.ebutilities.at/schemata/customerprocesses/common/types/01p20">
                    <ns0:MarketParticipantDirectory DocumentMode="PROD" Duplicate="false" SchemaVersion="01.41">
                        <ns1:RoutingHeader>
                            <ns1:Sender AddressType="ECNumber">
                                <ns1:MessageAddress>AT000000</ns1:MessageAddress>
                            </ns1:Sender>
                            <ns1:Receiver AddressType="ECNumber">
                                <ns1:MessageAddress>EP100000</ns1:MessageAddress>
                            </ns1:Receiver>
                            <ns1:DocumentCreationDateTime>2025-11-12T23:03:07.3687270Z</ns1:DocumentCreationDateTime>
                        </ns1:RoutingHeader>
                        <ns1:Sector>01</ns1:Sector>
                        <ns0:MessageCode>DATEN_CRMSG</ns0:MessageCode>
                    </ns0:MarketParticipantDirectory>
                    <ns0:ProcessDirectory>
                        <ns1:MessageId>AT000000000000000000000000000000000</ns1:MessageId>
                        <ns1:ConversationId>EP00000000000000000000</ns1:ConversationId>
                        <ns1:ProcessDate>2025-11-13</ns1:ProcessDate>
                        <ns1:MeteringPoint>AT0030000000000000000000000123456</ns1:MeteringPoint>
                        <ns0:Energy>
                            <ns0:MeteringReason>01</ns0:MeteringReason>
                            <ns0:MeteringPeriodStart>2025-02-28T00:00:00+01:00</ns0:MeteringPeriodStart>
                            <ns0:MeteringPeriodEnd>2025-05-01T00:00:00+02:00</ns0:MeteringPeriodEnd>
                            <ns0:MeteringIntervall>V</ns0:MeteringIntervall>
                            <ns0:NumberOfMeteringIntervall>1</ns0:NumberOfMeteringIntervall>
                            <ns0:EnergyData MeterCode="1-1:1.9.0" UOM="KWH">
                                <ns0:EP>
                                    <ns0:DTF>2025-02-28T00:00:00+01:00</ns0:DTF>
                                    <ns0:DTT>2025-05-01T00:00:00+02:00</ns0:DTT>
                                    <ns0:MM>01</ns0:MM>
                                    <ns0:BQ>3923.3</ns0:BQ>
                                </ns0:EP>
                            </ns0:EnergyData>
                        </ns0:Energy>
                    </ns0:ProcessDirectory>
                </ns0:ConsumptionRecord>
                """;
        var marshaller = JAXBContext.newInstance(ConsumptionRecord.class);
        var consumptionRecord = (ConsumptionRecord) marshaller.createUnmarshaller()
                                                              .unmarshal(new StringReader(payload));
        var edaConsumptionRecord = new EdaConsumptionRecord01p41(consumptionRecord);
        var identifiable = new IdentifiableConsumptionRecord(
                edaConsumptionRecord,
                List.of(new SimplePermissionRequest("pid", "cid", "dnid")),
                edaConsumptionRecord.startDate(),
                edaConsumptionRecord.endDate()
        );
        var intermediate = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, identifiable);

        var res = intermediate.toVhd();

        assertEquals(1, res.size());
    }

    private static Stream<Arguments> meterCodeAndMeteringModeSource() {
        return Stream.of(
                Arguments.of("1-1:1.9.0 P.01",
                             StandardDirectionTypeList.DOWN,
                             "L1",
                             StandardQualityTypeList.AS_PROVIDED),
                Arguments.of("1-1:2.9.0 P.01", StandardDirectionTypeList.UP, "L2", StandardQualityTypeList.ADJUSTED),
                Arguments.of("1-1:2.9.0 P.01", StandardDirectionTypeList.UP, "L3", StandardQualityTypeList.ESTIMATED)
        );
    }

    @Test
    void preservesUnknownOptionalFieldsAndFansOutEveryPermissionWithDocumentTimeFallback() {
        var start = LocalDate.of(2025, 1, 1);
        var created = start.atStartOfDay(AT_ZONE_ID);
        var raw = new SimpleEdaConsumptionRecord()
                .setMessageId("source-id")
                .setStartDate(start)
                .setEndDate(start.plusDays(1))
                .setDocumentCreationDateTime(created)
                .setSenderMessageAddress("sender")
                .setReceiverMessageAddress("receiver")
                .setMeteringPoint("meter")
                .setEnergy(List.of(new SimpleEnergy()
                        .setGranularity(Granularity.P1D)
                        .setMeterReadingStart(created)
                        .setMeterReadingEnd(created.plusDays(1))
                        .setEnergyData(List.of(new SimpleEnergyData()
                                .setMeterCode("1-1:9.9.0")
                                .setBillingUnit("unknown")
                                .setEnergyPositions(List.of(new EnergyPosition(new BigDecimal("1.2345"), "unknown")))))));
        var input = new IdentifiableConsumptionRecord(raw,
                List.of(new SimplePermissionRequest("first", "cid", "dnid"),
                        new SimplePermissionRequest("second", "cid", "dnid")), start, start.plusDays(1));

        var documents = new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, input).toVhd();

        assertEquals(2, documents.size());
        assertEquals(List.of("first", "second"), documents.stream()
                .map(document -> document.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId()).toList());
        var document = documents.getFirst().getVHDMarketDocument();
        assertEquals("source-id", document.getMRID());
        var series = document.getTimeSeries().getFirst();
        assertEquals(created, series.getDateAndOrTimeDateTime());
        assertEquals("1-1:9.9.0", series.getRegisteredResourceMRID().getValue());
        assertNull(series.getEnergyMeasurementUnitName());
        assertNull(series.getFlowDirectionDirection());
        var point = series.getSeriesPeriods().getFirst().getPoints().getFirst();
        assertEquals(new BigDecimal("1.2345"), point.getEnergyQuantityQuantity());
        assertNull(point.getEnergyQuantityQuality());
        assertNull(point.getEnergyQuantityQuantityDefinitionQuantityType());
    }

    private static void assertHistoricalDataRoundTrips(Envelope document) {
        assertNotNull(document.getVHDMarketDocument());
        assertFalse(document.getVHDMarketDocument().getTimeSeries().isEmpty());
        var xml = assertDoesNotThrow(XmlMessageSerde::new);
        var serialized = assertDoesNotThrow(() -> xml.serialize(document));
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(serialized));
        var xmlRoundTrip = assertDoesNotThrow(() -> xml.deserialize(serialized, Envelope.class));
        var json = new JsonMessageSerde();
        var jsonRoundTrip = assertDoesNotThrow(() -> json.deserialize(json.serialize(document), Envelope.class));
        assertEquals(document.getVHDMarketDocument().getMRID(), xmlRoundTrip.getVHDMarketDocument().getMRID());
        assertEquals(document.getVHDMarketDocument().getMRID(), jsonRoundTrip.getVHDMarketDocument().getMRID());
        var expectedSeries = document.getVHDMarketDocument().getTimeSeries();
        assertEquals(expectedSeries.size(), xmlRoundTrip.getVHDMarketDocument().getTimeSeries().size());
        assertEquals(expectedSeries.size(), jsonRoundTrip.getVHDMarketDocument().getTimeSeries().size());
        for (var index = 0; index < expectedSeries.size(); index++) {
            var expected = expectedSeries.get(index);
            assertFalse(expected.getSeriesPeriods().isEmpty());
            var fromXml = xmlRoundTrip.getVHDMarketDocument().getTimeSeries().get(index);
            var fromJson = jsonRoundTrip.getVHDMarketDocument().getTimeSeries().get(index);
            assertEquals(expected.getSeriesPeriods().size(), fromXml.getSeriesPeriods().size());
            assertEquals(expected.getSeriesPeriods().size(), fromJson.getSeriesPeriods().size());
            assertEquals(expected.getDateAndOrTimeDateTime().toInstant(), fromXml.getDateAndOrTimeDateTime().toInstant());
            assertEquals(expected.getDateAndOrTimeDateTime().toInstant(), fromJson.getDateAndOrTimeDateTime().toInstant());
            for (var period = 0; period < expected.getSeriesPeriods().size(); period++) {
                var points = expected.getSeriesPeriods().get(period).getPoints();
                assertFalse(points.isEmpty());
                var xmlPoints = fromXml.getSeriesPeriods().get(period).getPoints();
                var jsonPoints = fromJson.getSeriesPeriods().get(period).getPoints();
                assertEquals(points.size(), xmlPoints.size());
                assertEquals(points.size(), jsonPoints.size());
                for (var point = 0; point < points.size(); point++) {
                    assertEquals(points.get(point).getPosition(), xmlPoints.get(point).getPosition());
                    assertEquals(points.get(point).getPosition(), jsonPoints.get(point).getPosition());
                    assertEquals(points.get(point).getEnergyQuantityQuantity(), xmlPoints.get(point).getEnergyQuantityQuantity());
                    assertEquals(points.get(point).getEnergyQuantityQuantity(), jsonPoints.get(point).getEnergyQuantityQuantity());
                    assertEquals(points.get(point).getEnergyQualityQuantityQuantity(), xmlPoints.get(point).getEnergyQualityQuantityQuantity());
                    assertEquals(points.get(point).getEnergyQualityQuantityQuantity(), jsonPoints.get(point).getEnergyQualityQuantityQuantity());
                }
            }
        }
    }
}
