// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_12;

import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v1_12.StandardBusinessTypeList;
import energy.eddie.cim.v1_12.StandardDirectionTypeList;
import energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList;
import energy.eddie.cim.v1_12.vhd.*;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.cim.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.datatype.DatatypeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    @ParameterizedTest
    @MethodSource
    void testToVhds_returnsVhds(
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum format,
            StandardBusinessTypeList businessType,
            StandardDirectionTypeList flowDirection,
            CommodityKind commodity,
            StandardUnitOfMeasureTypeList unit,
            BigDecimal value
    ) {
        var businessTypeStr = businessType == null ? null : businessType.value();
        var unitStr = unit == null ? null : unit.value();

        var now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var acc = createAccount(900, format, now);
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .setDataStart(now.toLocalDate())
                .setDataEnd(now.toLocalDate())
                .build();
        var intermediateDocs = new IntermediateValidatedHistoricalDataMarketDocument(pr, List.of(acc, acc));

        var res = intermediateDocs.toVhds();

        assertEquals(2, res.size());
        assertNotEquals(res.get(0).getVHDMarketDocument().getMRID(), res.get(1).getVHDMarketDocument().getMRID());
        var series = res.getFirst().getVHDMarketDocument().getTimeSeries().getFirst();
        assertEquals(businessTypeStr, series.getBusinessType());
        assertEquals(unitStr, series.getEnergyMeasurementUnitName());
        assertEquals(commodity, series.getAccountingPointMeterReadingsReadingsReadingTypeCommodity());
        assertEquals(flowDirection.value(), series.getFlowDirectionDirection());
        assertEquals(value, series.getSeriesPeriods().getFirst().getPoints().getFirst().getEnergyQuantityQuantity());
        assertHistoricalDataRoundTrips(res.getFirst());
    }

    @Test
    void testToVhds_withUnknownGranularity_returnsVhds() {
        var now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var acc = createAccount(1, UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_FWD, now);
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .setDataStart(now.toLocalDate())
                .setDataEnd(now.toLocalDate())
                .build();
        var intermediateDocs = new IntermediateValidatedHistoricalDataMarketDocument(pr, List.of(acc));
        var expected = DatatypeFactory.newDefaultInstance().newDuration(Duration.ofSeconds(1).toMillis());

        var res = intermediateDocs.toVhds();

        assertThat(res)
                .singleElement()
                .extracting(Envelope::getVHDMarketDocument)
                .extracting(VHDMarketDocument::getTimeSeries)
                .asInstanceOf(InstanceOfAssertFactories.list(TimeSeries.class))
                .singleElement()
                .extracting(TimeSeries::getSeriesPeriods)
                .asInstanceOf(InstanceOfAssertFactories.list(SeriesPeriod.class))
                .singleElement()
                .extracting(SeriesPeriod::getResolution)
                .isEqualTo(expected);
    }

    @Test
    void testToVhds_removesDataOutsideOfStartAndEnd() {
        var now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var acc = createAccount(900, UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.DEMAND_KW, now);
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .setDataStart(now.plusDays(1).toLocalDate())
                .setDataEnd(now.plusDays(2).toLocalDate())
                .build();
        var intermediateDocs = new IntermediateValidatedHistoricalDataMarketDocument(pr, List.of(acc, acc));

        var res = intermediateDocs.toVhds();

        assertThat(res)
                .hasSize(2)
                .first()
                .extracting(Envelope::getVHDMarketDocument)
                .extracting(VHDMarketDocument::getTimeSeries)
                .asInstanceOf(InstanceOfAssertFactories.list(TimeSeries.class))
                .singleElement()
                .extracting(TimeSeries::getSeriesPeriods)
                .asInstanceOf(InstanceOfAssertFactories.list(SeriesPeriod.class))
                .singleElement()
                .extracting(SeriesPeriod::getPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(Point.class))
                .isEmpty();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testToVhds_returnsVhds() {
        return Stream.of(
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_FWD,
                             StandardBusinessTypeList.CONSUMPTION,
                             StandardDirectionTypeList.DOWN,
                             CommodityKind._2,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
                             BigDecimal.ONE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_REV,
                             StandardBusinessTypeList.PRODUCTION,
                             StandardDirectionTypeList.UP,
                             CommodityKind._2,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
                             BigDecimal.ONE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_NET,
                             null,
                             StandardDirectionTypeList.UP_AND_DOWN,
                             CommodityKind._2,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
                             BigDecimal.ONE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.WATER_GAL,
                             StandardBusinessTypeList.CONSUMPTION,
                             StandardDirectionTypeList.DOWN,
                             CommodityKind._9,
                             StandardUnitOfMeasureTypeList.CUBIC_METRE,
                             CimUnitConverter.GAL_TO_CUBIC_METRE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_CCF,
                             StandardBusinessTypeList.CONSUMPTION,
                             StandardDirectionTypeList.DOWN,
                             CommodityKind._7,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR,
                             CimUnitConverter.CCF_TO_KWH),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.EACS,
                             StandardBusinessTypeList.CONSUMPTION,
                             StandardDirectionTypeList.DOWN,
                             CommodityKind._0,
                             StandardUnitOfMeasureTypeList.ONE,
                             BigDecimal.ONE)
        );
    }

    private static Account createAccount(
            int interval,
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum format,
            ZonedDateTime timestamp
    ) {
        var usageSegment = new UsageSegment(
                timestamp,
                timestamp,
                BigDecimal.valueOf(interval),
                Map.of(format, List.of(BigDecimal.ONE))
        );
        var meter = new Meter("meter-number", "cds-id", List.of(usageSegment));
        return new Account("customer-number",
                           "customer name",
                           "business",
                           List.of(new ServiceContract(
                                   "",
                                   "",
                                   List.of(new ServicePoint("", List.of(meter)))
                           ))
        );
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
