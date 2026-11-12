// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_12;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_12.*;
import energy.eddie.cim.v1_12.vhd.*;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.providers.cim.Account;
import energy.eddie.regionconnector.cds.providers.cim.Meter;
import energy.eddie.regionconnector.cds.providers.cim.UsageSegment;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_12.vhd.VhdEnvelopeWrapper;
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

    public List<Envelope> toVhds() {
        return accounts.stream()
                       .map(this::toVhds)
                       .flatMap(List::stream)
                       .toList();
    }

    public List<Envelope> toVhds(Account account) {
        var created = ZonedDateTime.now(ZoneOffset.UTC);
        List<Envelope> envelopes = new ArrayList<>();
        for (var meter : account.meters()) {
            for (var usageSegment : meter.usageSegments()) {
                var interval = new EsmpTimeInterval(usageSegment.start(), usageSegment.end());
                var vhd = getVhd(account, created)
                        .withTimeSeries(getTimeSeriesList(meter, usageSegment, interval, created));
                envelopes.add(new VhdEnvelopeWrapper(vhd, permissionRequest).wrap());
            }
        }
        return envelopes;
    }

    private List<TimeSeries> getTimeSeriesList(
            Meter meter,
            UsageSegment usageSegment,
            EsmpTimeInterval interval,
            ZonedDateTime created
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
                    // dateAndOrTime.dateTime is mandatory in CIM v1.12, use the creation time of the document
                    .withDateAndOrTimeDateTime(created)
                    .withBusinessType(getBusinessType(direction))
                    .withProduct(StandardEnergyProductTypeList.ACTIVE_POWER.value())
                    .withFlowDirectionDirection(direction.value())
                    .withAccountingPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind._26)
                    .withAccountingPointMeterReadingsReadingsReadingTypeCommodity(getCommodity(format))
                    .withEnergyMeasurementUnitName(unit == null ? null : unit.value())
                    .withAccountingPointMRID(
                            new MeasurementPointIDString()
                                    .withCodingScheme(getCodingScheme())
                                    .withValue(meter.meterNumber())
                    )
                    .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                    .withSeriesPeriods(
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
                 DEMAND_KW, SUPPLY_MIX -> CommodityKind._2;
            case WATER_M3, WATER_GAL, WATER_FT3 -> CommodityKind._9;
            case GAS_THERM, GAS_CCF, GAS_MCF, GAS_MMBTU -> CommodityKind._7;
            case EACS -> CommodityKind._0;
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
            var point = new Point()
                    .withPosition(i + 1)
                    .withEnergyQuantityQuantity(value)
                    .withEnergyQuantityQuality(StandardQualityTypeList.AS_PROVIDED.value());
            points.add(point);
        }
        return points;
    }

    private static StandardDirectionTypeList getFlowDirection(FormatEnum format) {
        return switch (format) {
            case KWH_NET, USAGE_KWH, USAGE_NET_KWH -> StandardDirectionTypeList.UP_AND_DOWN;
            case KWH_REV, USAGE_REV_KWH -> StandardDirectionTypeList.UP;
            default -> StandardDirectionTypeList.DOWN;
        };
    }

    @Nullable
    private static String getBusinessType(StandardDirectionTypeList flowDirection) {
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
                .withRevisionNumber(CommonInformationModelVersions.V1_12.cimify())
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
