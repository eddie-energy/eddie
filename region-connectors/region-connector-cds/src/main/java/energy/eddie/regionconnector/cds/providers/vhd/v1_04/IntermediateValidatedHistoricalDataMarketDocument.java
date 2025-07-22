package energy.eddie.regionconnector.cds.providers.vhd.v1_04;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.DirectionTypeList;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.providers.cim.Account;
import energy.eddie.regionconnector.cds.providers.cim.Meter;
import energy.eddie.regionconnector.cds.providers.cim.UsageSegment;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

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

    public List<VHDEnvelope> toVhds() {
        return accounts.stream()
                       .map(this::toVhds)
                       .flatMap(List::stream)
                       .toList();
    }

    public List<VHDEnvelope> toVhds(Account account) {
        var created = ZonedDateTime.now(ZoneOffset.UTC);
        List<VHDEnvelope> envelopes = new ArrayList<>();
        for (var meter : account.meters()) {
            for (var usageSegment : meter.usageSegments()) {
                var interval = new EsmpTimeInterval(usageSegment.start(), usageSegment.end());
                var vhd = getVhd(account, created)
                        .withPeriodTimeInterval(new ESMPDateTimeInterval()
                                                        .withStart(interval.start())
                                                        .withEnd(interval.end()))
                        .withTimeSeries(getTimeSeriesList(meter, usageSegment, interval));
                envelopes.add(new VhdEnvelopeWrapper(vhd, permissionRequest).wrap());
            }
        }
        return envelopes;
    }

    private List<TimeSeries> getTimeSeriesList(
            Meter meter,
            UsageSegment usageSegment,
            EsmpTimeInterval interval
    ) {
        List<TimeSeries> timeSeriesList = new ArrayList<>();
        var resolution = getResolution(usageSegment);
        for (var entry : usageSegment.usageSegmentValues().entrySet()) {
            var format = entry.getKey();
            var converter = new CimUnitConverter(format);
            var direction = getFlowDirection(format);
            var unit = converter.unit();
            var timeSeries = new TimeSeries()
                    .withVersion("1")
                    .withMRID(UUID.randomUUID().toString())
                    .withBusinessType(getBusinessType(direction))
                    .withProduct(StandardEnergyProductTypeList.ACTIVE_POWER.value())
                    .withFlowDirectionDirection(direction.value())
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind.SUM)
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(getCommodity(format))
                    .withEnergyMeasurementUnitName(unit == null ? null : unit.value())
                    .withMarketEvaluationPointMRID(
                            new MeasurementPointIDString()
                                    .withCodingScheme(getCodingScheme())
                                    .withValue(meter.meterNumber())
                    )
                    .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                    .withPeriods(
                            new SeriesPeriod()
                                    .withResolution(resolution)
                                    .withTimeInterval(
                                            new ESMPDateTimeInterval()
                                                    .withStart(interval.start())
                                                    .withEnd(interval.end())
                                    )
                                    .withPoints(
                                            toPoints(usageSegment,
                                                     entry.getValue(),
                                                     converter)
                                    )
                    );
            timeSeriesList.add(timeSeries);
        }
        return timeSeriesList;
    }

    private CommodityKind getCommodity(FormatEnum format) {
        return switch (format) {
            case KWH_NET, KWH_FWD, KWH_REV, USAGE_KWH, USAGE_FWD_KWH, USAGE_REV_KWH, USAGE_NET_KWH, AGGREGATED_KWH,
                 DEMAND_KW, SUPPLY_MIX -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case WATER_M3, WATER_GAL, WATER_FT3 -> CommodityKind.POTABLEWATER;
            case GAS_THERM, GAS_CCF, GAS_MCF, GAS_MMBTU -> CommodityKind.NATURALGAS;
            case EACS -> CommodityKind.NONE;
        };
    }

    @Nullable
    private String getCodingScheme() {
        var countryCode = permissionRequest.dataSourceInformation().countryCode();
        return CimUtils.getCodingSchemeVhdV104(countryCode);
    }

    // Same mapping as for CIM v0.82
    @SuppressWarnings("DuplicatedCode")
    private List<Point> toPoints(
            UsageSegment usageSegment,
            List<BigDecimal> values,
            CimUnitConverter converter
    ) {
        List<Point> points = new ArrayList<>();
        var start = permissionRequest.start().atStartOfDay(ZoneOffset.UTC);
        var end = endOfDay(permissionRequest.end(), ZoneOffset.UTC);
        for (var i = 0; i < values.size(); i++) {
            var value = converter.convert(values.get(i));
            var offset = usageSegment.interval().multiply(BigDecimal.valueOf(i)).longValue();
            var timestamp = usageSegment.start().toInstant().plusSeconds(offset);
            var dateTime = timestamp.atZone(ZoneOffset.UTC);
            if (dateTime.isAfter(end) || dateTime.isBefore(start)) {
                continue;
            }
            var position = timestamp.getEpochSecond();
            var point = new Point()
                    .withPosition((int) position)
                    .withEnergyQuantityQuantity(value)
                    .withEnergyQuantityQuality(StandardQualityTypeList.AS_PROVIDED.value());
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
    private static String getBusinessType(DirectionTypeList flowDirection) {
        return switch (flowDirection) {
            case UP -> StandardBusinessTypeList.PRODUCTION.value();
            case DOWN -> StandardBusinessTypeList.CONSUMPTION.value();
            case UP_AND_DOWN, STABLE -> null;
        };
    }

    private static javax.xml.datatype.Duration getResolution(UsageSegment usageSegment) {
        var duration = Duration.ofSeconds(usageSegment.interval().longValue());
        return DatatypeFactory.newDefaultInstance().newDuration(duration.toMillis());
    }

    private VHDMarketDocument getVhd(Account account, ZonedDateTime created) {
        var codingScheme = getCodingScheme();
        return new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withCreatedDateTime(created)
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(codingScheme)
                                .withValue("CDSC")
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(codingScheme)
                                .withValue(account.cdsCustomerNumber())
                );
    }
}
