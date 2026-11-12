// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.testing;

import energy.eddie.cim.v1_04.vhd.QuantityTypeKind;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidatedHistoricalDataVersionComparatorTest {
    private static final ZonedDateTime CREATED = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final String PT15M = "PT15M";

    @Test
    void differences_forEquivalentDocuments_isEmpty() {
        // Given
        var v104 = v104();
        var v112 = v112();

        // When
        var res = ValidatedHistoricalDataVersionComparator.differences(v104, v112);

        // Then
        assertEquals(List.of(), res);
    }

    @Test
    void differences_forDocumentsWithoutOptionalFields_isEmpty() {
        // Given
        var v104 = new VHDEnvelope()
                .withMarketDocument(new energy.eddie.cim.v1_04.vhd.VHDMarketDocument()
                                            .withTimeSeries(new energy.eddie.cim.v1_04.vhd.TimeSeries()
                                                                    .withPeriods(new energy.eddie.cim.v1_04.vhd.SeriesPeriod()
                                                                                         .withPoints(new energy.eddie.cim.v1_04.vhd.Point()))));
        var v112 = new Envelope()
                .withMessageDocumentHeader(new energy.eddie.cim.v1_12.vhd.MessageDocumentHeader()
                                                   .withMetaInformation(new energy.eddie.cim.v1_12.vhd.MetaInformation()))
                .withVHDMarketDocument(new energy.eddie.cim.v1_12.vhd.VHDMarketDocument()
                                               .withTimeSeries(new energy.eddie.cim.v1_12.vhd.TimeSeries()
                                                                       .withSeriesPeriods(new energy.eddie.cim.v1_12.vhd.SeriesPeriod()
                                                                                                  .withPoints(new energy.eddie.cim.v1_12.vhd.Point()))));

        // When
        var res = ValidatedHistoricalDataVersionComparator.differences(v104, v112);

        // Then
        assertEquals(List.of(), res);
    }

    @Test
    void differences_reportsDifferentFieldsOnEveryLevel() {
        // Given
        var v104 = v104();
        var v112 = v112();
        v112.getMessageDocumentHeader().getMetaInformation().setConnectionId("other");
        v112.getVHDMarketDocument().getSenderMarketParticipantMRID().setValue("other");
        var timeSeries = v112.getVHDMarketDocument().getTimeSeries().getFirst();
        timeSeries.setFlowDirectionDirection("A01");
        var period = timeSeries.getSeriesPeriods().getFirst();
        period.getTimeInterval().setEnd("2026-01-02T00:00Z");
        period.getPoints().getFirst().setEnergyQuantityQuantity(BigDecimal.TEN);

        // When
        var res = ValidatedHistoricalDataVersionComparator.differences(v104, v112);

        // Then
        assertEquals(List.of(
                "connectionId: v1.04=<cid> v1.12=<other>",
                "sender.mRID: v1.04=<sender> v1.12=<other>",
                "timeSeries[0].flowDirection: v1.04=<A02> v1.12=<A01>",
                "timeSeries[0].periods[0].timeInterval.end: v1.04=<2026-01-01T00:15Z> v1.12=<2026-01-02T00:00Z>",
                "timeSeries[0].periods[0].points[0].energyQuantity.quantity: v1.04=<1.50> v1.12=<10>"
        ), res);
    }

    @Test
    void differences_treatsDecimalsWithDifferentScalesAsEqual() {
        // Given
        var v104 = v104();
        var v112 = v112();
        v112.getVHDMarketDocument()
            .getTimeSeries()
            .getFirst()
            .getSeriesPeriods()
            .getFirst()
            .getPoints()
            .getFirst()
            .setEnergyQuantityQuantity(new BigDecimal("1.5"));

        // When
        var res = ValidatedHistoricalDataVersionComparator.differences(v104, v112);

        // Then
        assertEquals(List.of(), res);
    }

    @Test
    void differences_ignoresDateAndOrTime_ifNotSetInV104() {
        // Given
        var v104 = v104();
        v104.getMarketDocument().getTimeSeries().getFirst().setDateAndOrTimeDateTime(null);
        var v112 = v112();

        // When
        var res = ValidatedHistoricalDataVersionComparator.differences(v104, v112);

        // Then
        assertEquals(List.of(), res);
    }

    @Test
    void differences_reportsDifferentListSizes() {
        // Given
        var v104 = v104();
        var v112 = v112();
        var period112 = v112.getVHDMarketDocument().getTimeSeries().getFirst().getSeriesPeriods().getFirst();
        period112.getPoints().add(new energy.eddie.cim.v1_12.vhd.Point().withPosition(2));
        var otherV104 = v104();
        otherV104.getMarketDocument().getTimeSeries().getFirst().getPeriods().add(
                new energy.eddie.cim.v1_04.vhd.SeriesPeriod()
        );
        var otherV112 = v112();
        otherV112.getVHDMarketDocument().getTimeSeries().add(new energy.eddie.cim.v1_12.vhd.TimeSeries());

        // When
        var points = ValidatedHistoricalDataVersionComparator.differences(v104, v112);
        var periods = ValidatedHistoricalDataVersionComparator.differences(otherV104, v112());
        var timeSeries = ValidatedHistoricalDataVersionComparator.differences(v104(), otherV112);

        // Then
        assertEquals(List.of("timeSeries[0].periods[0].points.size: v1.04=<1> v1.12=<2>"), points);
        assertEquals(List.of("timeSeries[0].periods.size: v1.04=<2> v1.12=<1>"), periods);
        assertEquals(List.of("timeSeries.size: v1.04=<1> v1.12=<2>"), timeSeries);
    }

    @Test
    void differences_reportsMissingMetaInformationAndMarketDocument() {
        // Given
        var withoutMeta = new Envelope();
        var withoutDocument = v112().withVHDMarketDocument(null);

        // When
        var missingMeta = ValidatedHistoricalDataVersionComparator.differences(v104(), withoutMeta);
        var missingDocument = ValidatedHistoricalDataVersionComparator.differences(v104(), withoutDocument);

        // Then
        assertEquals(List.of("v1.12 envelope has no meta information"), missingMeta);
        assertEquals(List.of("marketDocument present: v1.04=<true> v1.12=<false>"), missingDocument);
    }

    @Test
    void differencesIgnoring_removesIgnoredFields() {
        // Given
        var v104 = v104();
        var v112 = v112();
        v112.getVHDMarketDocument().setCreatedDateTime(CREATED.plusDays(1));
        v112.getVHDMarketDocument().getTimeSeries().getFirst().setDateAndOrTimeDateTime(CREATED.plusDays(1));
        v112.getVHDMarketDocument().setType("other");

        // When
        var res = ValidatedHistoricalDataVersionComparator.differencesIgnoring(v104,
                                                                               v112,
                                                                               "createdDateTime",
                                                                               "dateAndOrTime");

        // Then
        assertEquals(List.of("type: v1.04=<A45> v1.12=<other>"), res);
    }

    private static VHDEnvelope v104() {
        return new VHDEnvelope()
                .withMessageDocumentHeaderMetaInformationConnectionId("cid")
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMessageDocumentHeaderMetaInformationDataNeedId("dnid")
                .withMessageDocumentHeaderMetaInformationDocumentType("validated-historical-data-market-document")
                .withMessageDocumentHeaderMetaInformationRegionConnector("at-eda")
                .withMarketDocument(
                        new energy.eddie.cim.v1_04.vhd.VHDMarketDocument()
                                .withCreatedDateTime(CREATED)
                                .withDescription("description")
                                .withType("A45")
                                .withProcessProcessType("A16")
                                .withSenderMarketParticipantMRID(new energy.eddie.cim.v1_04.vhd.PartyIDString()
                                                                         .withValue("sender")
                                                                         .withCodingScheme("NAT"))
                                .withSenderMarketParticipantMarketRoleType("A26")
                                .withReceiverMarketParticipantMRID(new energy.eddie.cim.v1_04.vhd.PartyIDString()
                                                                           .withValue("receiver")
                                                                           .withCodingScheme("NAT"))
                                .withReceiverMarketParticipantMarketRoleType("A13")
                                .withTimeSeries(
                                        new energy.eddie.cim.v1_04.vhd.TimeSeries()
                                                .withVersion("1")
                                                .withBusinessType("A04")
                                                .withProduct("8716867000030")
                                                .withFlowDirectionDirection("A02")
                                                .withDateAndOrTimeDateTime(CREATED)
                                                .withEnergyMeasurementUnitName("KWH")
                                                .withEnergyQualityMeasurementUnitName("C62")
                                                .withReasonCode("999")
                                                .withReasonText("reason")
                                                .withRegisteredResourceMRID(new energy.eddie.cim.v1_04.vhd.ResourceIDString()
                                                                                    .withValue("resource")
                                                                                    .withCodingScheme("NAT"))
                                                .withRegisteredResourceName("name")
                                                .withRegisteredResourceDescription("description")
                                                .withRegisteredResourceFuelFuel("B01")
                                                .withRegisteredResourcePSRTypePsrType("A03")
                                                .withRegisteredResourceLocationMRID("location")
                                                .withMarketEvaluationPointMRID(new energy.eddie.cim.v1_04.vhd.MeasurementPointIDString()
                                                                                       .withValue("mp")
                                                                                       .withCodingScheme("NAT"))
                                                .withMarketEvaluationPointMeterReadingsMRID(new energy.eddie.cim.v1_04.vhd.ResourceIDString()
                                                                                                    .withValue("readings")
                                                                                                    .withCodingScheme("NAT"))
                                                .withMarketEvaluationPointMeterReadingsReadingsMRID(new energy.eddie.cim.v1_04.vhd.ResourceIDString()
                                                                                                            .withValue("reading")
                                                                                                            .withCodingScheme("NAT"))
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(energy.eddie.cim.v1_04.vhd.AggregateKind.SUM)
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulation(energy.eddie.cim.v1_04.vhd.AccumulationKind.DELTADATA)
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(energy.eddie.cim.v1_04.vhd.CommodityKind.ELECTRICITYPRIMARYMETERED)
                                                .withMarketEvaluationPointUsagePointLocationGeoInfoReference("geo")
                                                .withPeriods(
                                                        new energy.eddie.cim.v1_04.vhd.SeriesPeriod()
                                                                .withResolution(DatatypeFactory.newDefaultInstance().newDuration(PT15M))
                                                                .withTimeInterval(new energy.eddie.cim.v1_04.vhd.ESMPDateTimeInterval()
                                                                                          .withStart("2026-01-01T00:00Z")
                                                                                          .withEnd("2026-01-01T00:15Z"))
                                                                .withReasonCode("999")
                                                                .withReasonText("reason")
                                                                .withPoints(new energy.eddie.cim.v1_04.vhd.Point()
                                                                                    .withPosition(1)
                                                                                    .withEnergyQuantityQuantity(new BigDecimal("1.50"))
                                                                                    .withEnergyQuantityQuality("A04")
                                                                                    .withEnergyQuantityType(QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH)
                                                                                    .withEnergyQualityQuantityQuantity(BigDecimal.ONE)
                                                                                    .withEnergyQualityQuantityQuality("A04")
                                                                                    .withEnergyQualityQuantityType(QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH))
                                                )
                                )
                );
    }

    private static Envelope v112() {
        return new Envelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v1_12.vhd.MessageDocumentHeader()
                                .withMetaInformation(new energy.eddie.cim.v1_12.vhd.MetaInformation()
                                                             .withConnectionId("cid")
                                                             .withRequestPermissionId("pid")
                                                             .withDataNeedId("dnid")
                                                             .withDocumentType("validated-historical-data-market-document")
                                                             .withRegionConnector("at-eda"))
                )
                .withVHDMarketDocument(
                        new energy.eddie.cim.v1_12.vhd.VHDMarketDocument()
                                .withCreatedDateTime(CREATED)
                                .withDescription("description")
                                .withType("A45")
                                .withProcessProcessType("A16")
                                .withSenderMarketParticipantMRID(new energy.eddie.cim.v1_12.vhd.PartyIDString()
                                                                         .withValue("sender")
                                                                         .withCodingScheme("NAT"))
                                .withSenderMarketParticipantMarketRoleType("A26")
                                .withReceiverMarketParticipantMRID(new energy.eddie.cim.v1_12.vhd.PartyIDString()
                                                                           .withValue("receiver")
                                                                           .withCodingScheme("NAT"))
                                .withReceiverMarketParticipantMarketRoleType("A13")
                                .withTimeSeries(
                                        new energy.eddie.cim.v1_12.vhd.TimeSeries()
                                                .withVersion("1")
                                                .withBusinessType("A04")
                                                .withProduct("8716867000030")
                                                .withFlowDirectionDirection("A02")
                                                .withDateAndOrTimeDateTime(CREATED)
                                                .withEnergyMeasurementUnitName("KWH")
                                                .withEnergyQualityMeasurementUnitName("C62")
                                                .withReasonCode("999")
                                                .withReasonText("reason")
                                                .withRegisteredResourceMRID(new energy.eddie.cim.v1_12.vhd.ResourceIDString()
                                                                                    .withValue("resource")
                                                                                    .withCodingScheme("NAT"))
                                                .withRegisteredResourceName("name")
                                                .withRegisteredResourceDescription("description")
                                                .withRegisteredResourceFuelFuel("B01")
                                                .withRegisteredResourcePSRTypePsrType("A03")
                                                .withRegisteredResourceLocationMRID("location")
                                                .withAccountingPointMRID(new energy.eddie.cim.v1_12.vhd.MeasurementPointIDString()
                                                                                 .withValue("mp")
                                                                                 .withCodingScheme("NAT"))
                                                .withAccountingPointMeterReadingsMRID(new energy.eddie.cim.v1_12.vhd.ResourceIDString()
                                                                                              .withValue("readings")
                                                                                              .withCodingScheme("NAT"))
                                                .withAccountingPointMeterReadingsReadingsMRID(new energy.eddie.cim.v1_12.vhd.ResourceIDString()
                                                                                                      .withValue("reading")
                                                                                                      .withCodingScheme("NAT"))
                                                .withAccountingPointMeterReadingsReadingsReadingTypeAggregate(energy.eddie.cim.v1_12.vhd.AggregateKind._26)
                                                .withAccountingPointMeterReadingsReadingsReadingTypeAccumulation(energy.eddie.cim.v1_12.vhd.AccumulationKind._4)
                                                .withAccountingPointMeterReadingsReadingsReadingTypeCommodity(energy.eddie.cim.v1_12.vhd.CommodityKind._2)
                                                .withAccountingPointUsagePointLocationGeoInfoReference("geo")
                                                .withSeriesPeriods(
                                                        new energy.eddie.cim.v1_12.vhd.SeriesPeriod()
                                                                .withResolution(DatatypeFactory.newDefaultInstance().newDuration(PT15M))
                                                                .withTimeInterval(new energy.eddie.cim.v1_12.vhd.ESMPDateTimeInterval()
                                                                                          .withStart("2026-01-01T00:00Z")
                                                                                          .withEnd("2026-01-01T00:15Z"))
                                                                .withReasonCode("999")
                                                                .withReasonText("reason")
                                                                .withPoints(new energy.eddie.cim.v1_12.vhd.Point()
                                                                                    .withPosition(1)
                                                                                    .withEnergyQuantityQuantity(new BigDecimal("1.50"))
                                                                                    .withEnergyQuantityQuality("A04")
                                                                                    .withEnergyQuantityType("0")
                                                                                    .withEnergyQualityQuantityQuantity(BigDecimal.ONE)
                                                                                    .withEnergyQualityQuantityQuality("A04")
                                                                                    .withEnergyQualityQuantityType("0"))
                                                )
                                )
                );
    }
}
