package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


class IntermediateValidatedHistoricalDataMarketDocumentTest {

    static Stream<Arguments> testToVhds_returnsVhds() {
        return Stream.of(
                Arguments.of(FormatEnum.KWH_FWD,
                             BusinessTypeList.CONSUMPTION,
                             DirectionTypeList.DOWN,
                             CommodityKind.ELECTRICITYPRIMARYMETERED,
                             UnitOfMeasureTypeList.KILOWATT_HOUR,
                             BigDecimal.ONE),
                Arguments.of(FormatEnum.KWH_REV,
                             BusinessTypeList.PRODUCTION,
                             DirectionTypeList.UP,
                             CommodityKind.ELECTRICITYPRIMARYMETERED,
                             UnitOfMeasureTypeList.KILOWATT_HOUR,
                             BigDecimal.ONE),
                Arguments.of(FormatEnum.KWH_NET,
                             null,
                             DirectionTypeList.UP_AND_DOWN,
                             CommodityKind.ELECTRICITYPRIMARYMETERED,
                             UnitOfMeasureTypeList.KILOWATT_HOUR,
                             BigDecimal.ONE),
                Arguments.of(FormatEnum.WATER_GAL,
                             BusinessTypeList.CONSUMPTION,
                             DirectionTypeList.DOWN,
                             CommodityKind.POTABLEWATER,
                             UnitOfMeasureTypeList.CUBIC_METRE,
                             CimUnitConverter.GAL_TO_CUBIC_METRE),
                Arguments.of(FormatEnum.GAS_CCF,
                             BusinessTypeList.CONSUMPTION,
                             DirectionTypeList.DOWN,
                             CommodityKind.NATURALGAS,
                             UnitOfMeasureTypeList.KILOWATT_HOUR,
                             CimUnitConverter.CCF_TO_KWH),
                Arguments.of(FormatEnum.EACS,
                             BusinessTypeList.CONSUMPTION,
                             DirectionTypeList.DOWN,
                             null,
                             null,
                             BigDecimal.ONE)
        );
    }

    @ParameterizedTest
    @MethodSource
    @SuppressWarnings("java:S5961")
        // The mapping requires that many asserts and splitting in multiple tests doesn't make sense
    void testToVhds_returnsVhds(
            FormatEnum format,
            BusinessTypeList businessType,
            DirectionTypeList flowDirection,
            CommodityKind commodity,
            UnitOfMeasureTypeList unit,
            BigDecimal value
    ) {
        // Given
        var now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var usageSegment = new Account.UsageSegment(
                now,
                now,
                BigDecimal.valueOf(900),
                Map.of(format, List.of(BigDecimal.ONE))
        );
        var meter = new Account.Meter("meter-number", List.of(usageSegment)
        );
        var acc = new Account("customer-number", List.of(meter));
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .build();
        var intermediateDocs = new IntermediateValidatedHistoricalDataMarketDocument(pr, List.of(acc, acc));

        // When
        var res = intermediateDocs.toVhds();

        // Then
        assertThat(res)
                .hasSize(2)
                .first()
                .extracting(ValidatedHistoricalDataEnvelope::getValidatedHistoricalDataMarketDocument)
                .satisfies(vhd -> {
                    assertThat(vhd.getMRID()).isNotEmpty();
                    assertThat(vhd.getRevisionNumber()).isEqualTo("0.82");
                    assertThat(vhd.getType()).isEqualTo(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT);
                    assertThat(vhd.getCreatedDateTime()).isNotEmpty();
                    assertThat(vhd.getSenderMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.METERING_POINT_ADMINISTRATOR);
                    assertThat(vhd.getReceiverMarketParticipantMarketRoleType()).isEqualTo(RoleTypeList.CONSUMER);
                    assertThat(vhd.getProcessProcessType()).isEqualTo(ProcessTypeList.REALISED);
                    assertThat(vhd.getSenderMarketParticipantMRID()
                                  .getCodingScheme()).isEqualTo(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME);
                    assertThat(vhd.getSenderMarketParticipantMRID().getValue()).isNullOrEmpty();
                    assertThat(vhd.getReceiverMarketParticipantMRID()
                                  .getCodingScheme()).isEqualTo(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME);
                    assertThat(vhd.getReceiverMarketParticipantMRID().getValue()).isEqualTo(acc.cdsCustomerNumber());
                    assertThat(vhd.getPeriodTimeInterval().getStart()).isEqualTo("2025-01-01T00:00Z");
                    assertThat(vhd.getPeriodTimeInterval().getEnd()).isEqualTo("2025-01-01T00:00Z");
                })
                .extracting(ValidatedHistoricalDataMarketDocumentComplexType::getTimeSeriesList)
                .extracting(ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList::getTimeSeries)
                .asInstanceOf(InstanceOfAssertFactories.list(TimeSeriesComplexType.class))
                .singleElement()
                .satisfies(t -> {
                    assertThat(t.getMRID()).isNotEmpty();
                    assertThat(t.getBusinessType()).isEqualTo(businessType);
                    assertThat(t.getProduct()).isEqualTo(EnergyProductTypeList.ACTIVE_POWER);
                    assertThat(t.getFlowDirectionDirection()).isEqualTo(flowDirection);
                    assertThat(t.getMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation())
                            .isEqualTo(AggregateKind.SUM);
                    assertThat(t.getMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity())
                            .isEqualTo(commodity);
                    assertThat(t.getEnergyMeasurementUnitName()).isEqualTo(unit);
                    assertThat(t.getMarketEvaluationPointMRID().getCodingScheme())
                            .isEqualTo(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME);
                    assertThat(t.getMarketEvaluationPointMRID().getValue()).isEqualTo(meter.meterNumber());
                    assertThat(t.getReasonList().getReasons())
                            .singleElement()
                            .extracting(ReasonComplexType::getCode)
                            .isEqualTo(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED);
                })
                .extracting(TimeSeriesComplexType::getSeriesPeriodList)
                .extracting(TimeSeriesComplexType.SeriesPeriodList::getSeriesPeriods)
                .asInstanceOf(InstanceOfAssertFactories.list(SeriesPeriodComplexType.class))
                .singleElement()
                .satisfies(period -> {
                    assertThat(period.getResolution()).isEqualTo("PT15M");
                    assertThat(period.getTimeInterval().getStart()).isEqualTo("2025-01-01T00:00Z");
                    assertThat(period.getTimeInterval().getEnd()).isEqualTo("2025-01-01T00:00Z");
                })
                .extracting(SeriesPeriodComplexType::getPointList)
                .extracting(SeriesPeriodComplexType.PointList::getPoints)
                .asInstanceOf(InstanceOfAssertFactories.list(PointComplexType.class))
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.getPosition()).isEqualTo("" + now.toInstant().getEpochSecond());
                    assertThat(p.getEnergyQuantityQuantity()).isEqualTo(value);
                });
    }

    @Test
    void testToVhds_withUnknownGranularity_returnsVhds() {
        // Given
        var now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var usageSegment = new Account.UsageSegment(
                now,
                now,
                BigDecimal.valueOf(1),
                Map.of(FormatEnum.KWH_FWD, List.of(BigDecimal.ONE))
        );
        var meter = new Account.Meter("meter-number", List.of(usageSegment)
        );
        var acc = new Account("customer-number", List.of(meter));
        var pr = new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .build();
        var intermediateDocs = new IntermediateValidatedHistoricalDataMarketDocument(pr, List.of(acc));

        // When
        var res = intermediateDocs.toVhds();

        // Then
        assertThat(res)
                .singleElement()
                .extracting(ValidatedHistoricalDataEnvelope::getValidatedHistoricalDataMarketDocument)
                .extracting(ValidatedHistoricalDataMarketDocumentComplexType::getTimeSeriesList)
                .extracting(ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList::getTimeSeries)
                .asInstanceOf(InstanceOfAssertFactories.list(TimeSeriesComplexType.class))
                .singleElement()
                .extracting(TimeSeriesComplexType::getSeriesPeriodList)
                .extracting(TimeSeriesComplexType.SeriesPeriodList::getSeriesPeriods)
                .asInstanceOf(InstanceOfAssertFactories.list(SeriesPeriodComplexType.class))
                .singleElement()
                .extracting(SeriesPeriodComplexType::getResolution)
                .isEqualTo("1s");
    }
}