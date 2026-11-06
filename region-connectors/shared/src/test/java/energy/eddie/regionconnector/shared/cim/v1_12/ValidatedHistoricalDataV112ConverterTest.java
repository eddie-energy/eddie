// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12;

import energy.eddie.cim.serde.*;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v1_04.vhd.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import javax.xml.datatype.DatatypeFactory;

import static org.junit.jupiter.api.Assertions.*;

class ValidatedHistoricalDataV112ConverterTest {
    private static final ZonedDateTime CREATED = ZonedDateTime.parse("2026-01-02T03:04:05Z");

    @Test
    void preservesMeasurementsMetadataAndRequiredCreationTimeWithoutMutatingSource() throws Exception {
        var source = sample();
        var converted = ValidatedHistoricalDataV112Converter.convert(source);
        var meta = converted.getMessageDocumentHeader().getMetaInformation();
        assertEquals("pid", meta.getRequestPermissionId());
        assertEquals("cid", meta.getConnectionId());
        assertEquals("dnid", meta.getDataNeedId());
        assertEquals("at-eda", meta.getRegionConnector());
        assertEquals("AT", meta.getRegionCountry());
        var document = converted.getVHDMarketDocument();
        assertEquals("document-id", document.getMRID());
        assertEquals(CREATED, document.getCreatedDateTime());
        var series = document.getTimeSeries().getFirst();
        assertEquals("series-id", series.getMRID());
        assertEquals(CREATED, series.getDateAndOrTimeDateTime());
        assertNull(source.getMarketDocument().getTimeSeries().getFirst().getDateAndOrTimeDateTime());
        assertEquals("meter-id", series.getAccountingPointMRID().getValue());
        assertEquals("A01", series.getAccountingPointMRID().getCodingScheme());
        assertEquals("KWH", series.getEnergyMeasurementUnitName());
        assertEquals("KVR", series.getEnergyQualityMeasurementUnitName());
        var period = series.getSeriesPeriods().getFirst();
        assertEquals(source.getMarketDocument().getTimeSeries().getFirst().getPeriods().getFirst().getResolution(), period.getResolution());
        assertEquals("2026-01-02T03:04Z", period.getTimeInterval().getStart());
        assertEquals("2026-01-02T04:04Z", period.getTimeInterval().getEnd());
        var point = period.getPoints().getFirst();
        assertEquals(new BigDecimal("123456789.123456789"), point.getEnergyQuantityQuantity());
        assertEquals(new BigDecimal("0.000000001"), point.getEnergyQualityQuantityQuantity());
        assertEquals("A04", point.getEnergyQuantityQuality());
        assertEquals("A05", point.getEnergyQualityQuantityQuality());
        assertEquals("0", point.getEnergyQuantityType());
        assertEquals("reason", period.getReasonText());
        assertNotSame(source.getMarketDocument().getTimeSeries().getFirst().getPeriods().getFirst().getPoints().getFirst(), point);
        for (var format : new String[]{"xml", "json"}) {
            var serde = new DefaultSerdeFactory().create(format);
            var bytes = serde.serialize(converted);
            if (format.equals("xml")) {
                assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(bytes));
            }
            var roundTrip = serde.deserialize(bytes, energy.eddie.cim.v1_12.vhd.Envelope.class);
            assertArrayEquals(bytes, serde.serialize(roundTrip));
        }
    }

    @Test
    void retainsProviderProcessTimeAndMultipleSeries() throws Exception {
        var source = sample();
        var processTime = CREATED.minusDays(1);
        var first = source.getMarketDocument().getTimeSeries().getFirst();
        first.setDateAndOrTimeDateTime(processTime);
        source.getMarketDocument().getTimeSeries().add(new TimeSeries().withMRID("empty").withVersion("1").withFlowDirectionDirection("A02"));
        var target = ValidatedHistoricalDataV112Converter.convert(source).getVHDMarketDocument();
        assertEquals(2, target.getTimeSeries().size());
        assertEquals(processTime, target.getTimeSeries().getFirst().getDateAndOrTimeDateTime());
        assertTrue(target.getTimeSeries().getLast().getSeriesPeriods().isEmpty());
        assertNull(target.getTimeSeries().getLast().getAccountingPointMRID());
        assertEquals(CREATED, target.getTimeSeries().getLast().getDateAndOrTimeDateTime());
    }

    @Test
    void preservesOptionalResourceReadingIdentifiersAndCoordinates() throws Exception {
        var source = sample();
        var series = source.getMarketDocument().getTimeSeries().getFirst();
        series.withRegisteredResourceMRID(new ResourceIDString().withValue("resource").withCodingScheme("A01"))
                .withRegisteredResourceName("generator").withRegisteredResourceDescription("roof")
                .withRegisteredResourceLocationMRID("location")
                .withRegisteredResourceLocationCoordinateSystemCrsUrn("crs")
                .withRegisteredResourceLocationPositionPointsXPosition("14.12345")
                .withRegisteredResourceLocationPositionPointsYPosition("48.12345")
                .withRegisteredResourceLocationPositionPointsZPosition("300")
                .withMarketEvaluationPointMeterReadingsMRID(new ResourceIDString().withValue("meter-reading").withCodingScheme("A01"))
                .withMarketEvaluationPointMeterReadingsReadingsMRID(new ResourceIDString().withValue("reading").withCodingScheme("A01"))
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulation(AccumulationKind.fromValue("1"))
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind.fromValue("0"))
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.fromValue("1"))
                .withMarketEvaluationPointUsagePointLocationGeoInfoReference("geo")
                .withReasonCode("999").withReasonText("provider reason");
        var target = ValidatedHistoricalDataV112Converter.convert(source).getVHDMarketDocument().getTimeSeries().getFirst();
        assertEquals("resource", target.getRegisteredResourceMRID().getValue());
        assertEquals("A01", target.getRegisteredResourceMRID().getCodingScheme());
        assertEquals("generator", target.getRegisteredResourceName());
        assertEquals("roof", target.getRegisteredResourceDescription());
        assertEquals("location", target.getRegisteredResourceLocationMRID());
        assertEquals("crs", target.getRegisteredResourceLocationGeometryCoordinateSystemCrsUrn());
        assertEquals("14.12345", target.getRegisteredResourceLocationGeometryPositionPointXPosition());
        assertEquals("48.12345", target.getRegisteredResourceLocationGeometryPositionPointYPosition());
        assertEquals("300", target.getRegisteredResourceLocationGeometryPositionPointZPosition());
        assertEquals("meter-reading", target.getAccountingPointMeterReadingsMRID().getValue());
        assertEquals("reading", target.getAccountingPointMeterReadingsReadingsMRID().getValue());
        assertEquals("1", target.getAccountingPointMeterReadingsReadingsReadingTypeAccumulation().value());
        assertEquals("0", target.getAccountingPointMeterReadingsReadingsReadingTypeAggregate().value());
        assertEquals("1", target.getAccountingPointMeterReadingsReadingsReadingTypeCommodity().value());
        assertEquals("geo", target.getAccountingPointUsagePointLocationGeoInfoReference());
        assertEquals("provider reason", target.getReasonText());
    }

    @Test
    void supportsEmptyOptionalEnvelope() {
        var converted = ValidatedHistoricalDataV112Converter.convert(new VHDEnvelope());
        assertNull(converted.getVHDMarketDocument());
        assertNull(converted.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
    }

    private static VHDEnvelope sample() throws Exception {
        var period = new SeriesPeriod().withResolution(DatatypeFactory.newInstance().newDuration("PT15M"))
                .withTimeInterval(new ESMPDateTimeInterval().withStart("2026-01-02T03:04Z").withEnd("2026-01-02T04:04Z"))
                .withReasonCode("999").withReasonText("reason")
                .withPoints(new Point().withPosition(1).withEnergyQuantityQuantity(new BigDecimal("123456789.123456789"))
                        .withEnergyQuantityQuality("A04").withEnergyQuantityType(QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH)
                        .withEnergyQualityQuantityQuantity(new BigDecimal("0.000000001")).withEnergyQualityQuantityQuality("A05"));
        var series = new TimeSeries().withMRID("series-id").withVersion("1").withFlowDirectionDirection("A02")
                .withEnergyMeasurementUnitName("KWH").withEnergyQualityMeasurementUnitName("KVR")
                .withMarketEvaluationPointMRID(new MeasurementPointIDString().withValue("meter-id").withCodingScheme("A01"))
                .withPeriods(period);
        return new VHDEnvelope().withMessageDocumentHeaderCreationDateTime(CREATED)
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMessageDocumentHeaderMetaInformationConnectionId("cid")
                .withMessageDocumentHeaderMetaInformationDataNeedId("dnid")
                .withMessageDocumentHeaderMetaInformationRegionConnector("at-eda")
                .withMessageDocumentHeaderMetaInformationRegionCountry("AT")
                .withMarketDocument(new VHDMarketDocument().withMRID("document-id").withCreatedDateTime(CREATED)
                        .withProcessProcessType("A16")
                        .withSenderMarketParticipantMRID(new PartyIDString().withValue("sender").withCodingScheme("A01"))
                        .withReceiverMarketParticipantMRID(new PartyIDString().withValue("receiver").withCodingScheme("A01"))
                        .withSenderMarketParticipantMarketRoleType("A26").withReceiverMarketParticipantMarketRoleType("A13")
                        .withTimeSeries(series));
    }
}
