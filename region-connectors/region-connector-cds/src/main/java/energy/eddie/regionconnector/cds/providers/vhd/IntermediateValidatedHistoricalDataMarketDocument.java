package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class IntermediateValidatedHistoricalDataMarketDocument {
    private final CdsPermissionRequest permissionRequest;
    private final List<Account> accounts;

    public IntermediateValidatedHistoricalDataMarketDocument(
            CdsPermissionRequest permissionRequest,
            List<Account> accounts
    ) {
        this.permissionRequest = permissionRequest;
        this.accounts = accounts;
    }

    public List<ValidatedHistoricalDataEnvelope> toVhds() {
        return accounts.stream()
                       .map(this::toVhds)
                       .flatMap(List::stream)
                       .toList();
    }

    public List<ValidatedHistoricalDataEnvelope> toVhds(Account account) {
        EsmpDateTime created = EsmpDateTime.now();
        List<ValidatedHistoricalDataEnvelope> envelopes = new ArrayList<>();
        for (var meter : account.meters()) {
            for (var usageSegment : meter.usageSegments()) {
                var interval = new EsmpTimeInterval(usageSegment.start(), usageSegment.end());
                var vhd = getVhd(account, created)
                        .withPeriodTimeInterval(new ESMPDateTimeIntervalComplexType()
                                                        .withStart(interval.start())
                                                        .withEnd(interval.end()))
                        .withTimeSeriesList(
                                new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                        .withTimeSeries(
                                                getTimeSeriesList(meter, usageSegment, interval)
                                        )
                        );
                envelopes.add(new VhdEnvelope(vhd, permissionRequest).wrap());
            }
        }
        return envelopes;
    }

    private static List<TimeSeriesComplexType> getTimeSeriesList(
            Account.Meter meter,
            Account.UsageSegment usageSegment,
            EsmpTimeInterval interval
    ) {
        List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
        var resolution = getResolution(usageSegment);
        for (var entry : usageSegment.usageSegmentValues().entrySet()) {
            var format = entry.getKey();
            var direction = getFlowDirection(format);
            var timeSeries = new TimeSeriesComplexType()
                    .withMRID(UUID.randomUUID().toString())
                    .withBusinessType(getBusinessType(direction))
                    .withProduct(EnergyProductTypeList.ACTIVE_POWER)
                    .withFlowDirectionDirection(direction)
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(AggregateKind.SUM)
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                    .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT_HOUR)
                    .withMarketEvaluationPointMRID(
                            new MeasurementPointIDStringComplexType()
                                    .withCodingScheme(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME)
                                    .withValue(meter.meterNumber())
                    )
                    .withReasonList(
                            new TimeSeriesComplexType.ReasonList()
                                    .withReasons(new ReasonComplexType()
                                                         .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED))
                    )
                    .withSeriesPeriodList(
                            new TimeSeriesComplexType.SeriesPeriodList()
                                    .withSeriesPeriods(
                                            new SeriesPeriodComplexType()
                                                    .withResolution(resolution)
                                                    .withTimeInterval(
                                                            new ESMPDateTimeIntervalComplexType()
                                                                    .withStart(interval.start())
                                                                    .withEnd(interval.end())
                                                    )
                                                    .withPointList(
                                                            new SeriesPeriodComplexType.PointList()
                                                                    .withPoints(
                                                                            toPoints(usageSegment, entry.getValue())
                                                                    )
                                                    )
                                    )
                    );
            timeSeriesList.add(timeSeries);
        }
        return timeSeriesList;
    }

    private static List<PointComplexType> toPoints(
            Account.UsageSegment usageSegment,
            List<BigDecimal> values
    ) {
        List<PointComplexType> points = new ArrayList<>();
        for (var i = 0; i < values.size(); i++) {
            var value = values.get(i);
            var offset = usageSegment.interval().multiply(BigDecimal.valueOf(i)).longValue();
            var position = usageSegment.start().toInstant().plusSeconds(offset).getEpochSecond();
            var point = new PointComplexType()
                    .withPosition(String.valueOf(position))
                    .withEnergyQuantityQuantity(value)
                    .withEnergyQuantityQuality(QualityTypeList.AS_PROVIDED);
            points.add(point);
        }
        return points;
    }

    private static DirectionTypeList getFlowDirection(FormatEnum format) {
        return switch (format) {
            case KWH_NET, USAGE_KWH, USAGE_NET_KWH -> DirectionTypeList.UP_AND_DOWN;
            case KWH_REV, USAGE_REV_KWH -> DirectionTypeList.UP;
            default -> DirectionTypeList.DOWN;
        };
    }

    @Nullable
    private static BusinessTypeList getBusinessType(DirectionTypeList flowDirection) {
        return switch (flowDirection) {
            case UP -> BusinessTypeList.PRODUCTION;
            case DOWN -> BusinessTypeList.CONSUMPTION;
            case UP_AND_DOWN, STABLE -> null;
        };
    }

    @Nullable
    private static String getResolution(Account.UsageSegment usageSegment) {
        try {
            var duration = Duration.ofSeconds(usageSegment.interval().longValue());
            return Granularity.fromMinutes((int) duration.toMinutes()).toString();
        } catch (Exception e) {
            return usageSegment.interval().toString() + "s";
        }
    }

    private static ValidatedHistoricalDataMarketDocumentComplexType getVhd(Account account, EsmpDateTime created) {
        return new ValidatedHistoricalDataMarketDocumentComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
                .withCreatedDateTime(created.toString())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withProcessProcessType(ProcessTypeList.REALISED)
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.USA_NATIONAL_CODING_SCHEME)
                                .withValue(account.cdsCustomerNumber())
                );
    }
}
