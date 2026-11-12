// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
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
import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_12.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

class IntermediateValidatedHistoricalDataMarketDocument {
    private final HistoricalDataEmissionMetadata metadata;
    private final CdsPermissionRequest permissionRequest;
    private final List<Account> accounts;

    public IntermediateValidatedHistoricalDataMarketDocument(
            CdsPermissionRequest permissionRequest,
            List<Account> accounts
    ) {
        this(permissionRequest, accounts, new HistoricalDataEmissionMetadata());
    }

    public IntermediateValidatedHistoricalDataMarketDocument(
            CdsPermissionRequest permissionRequest,
            List<Account> accounts,
            HistoricalDataEmissionMetadata metadata
    ) {
        this.metadata = metadata;
        this.permissionRequest = permissionRequest;
        this.accounts = accounts;
    }

    public List<Envelope> toVhds() {
        var envelopes = new ArrayList<Envelope>();
        for (var accountIndex = 0; accountIndex < accounts.size(); accountIndex++) {
            envelopes.addAll(toVhds(accounts.get(accountIndex), accountIndex));
        }
        return envelopes;
    }

    public List<Envelope> toVhds(Account account) {
        return toVhds(account, accounts.indexOf(account));
    }

    private List<Envelope> toVhds(Account account, int accountIndex) {
        var created = metadata.created(ZoneOffset.UTC);
        List<Envelope> envelopes = new ArrayList<>();
        var meters = account.meters();
        for (var meterIndex = 0; meterIndex < meters.size(); meterIndex++) {
            var meter = meters.get(meterIndex);
            for (var segmentIndex = 0; segmentIndex < meter.usageSegments().size(); segmentIndex++) {
                var usageSegment = meter.usageSegments().get(segmentIndex);
                var sourceKey = "account:" + accountIndex + "/meter:" + meterIndex + "/segment:" + segmentIndex;
                var interval = new EsmpTimeInterval(usageSegment.start(), usageSegment.end());
                var vhd = getVhd(account, created, sourceKey)
                        .withTimeSeries(getTimeSeriesList(meter, usageSegment, interval, sourceKey));
                envelopes.add(new VhdEnvelopeWrapper(vhd, permissionRequest, metadata.envelopeCreated()).wrap());
            }
        }
        return envelopes;
    }

    private List<TimeSeries> getTimeSeriesList(
            Meter meter,
            UsageSegment usageSegment,
            EsmpTimeInterval interval,
            String sourceKey
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
                    .withMRID(metadata.child(sourceKey).id("format:" + format.name()))
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

    private VHDMarketDocument getVhd(Account account, ZonedDateTime created, String sourceKey) {
        var codingScheme = getCodingScheme();
        return new VHDMarketDocument()
                .withMRID(metadata.child(sourceKey).id("document"))
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
