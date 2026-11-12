// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_04;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.providers.cim.Account;
import energy.eddie.regionconnector.cds.providers.cim.Meter;
import energy.eddie.regionconnector.cds.providers.cim.UsageSegment;
import energy.eddie.regionconnector.cds.providers.vhd.CdsMeteringSeries;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
        var resolution = DatatypeFactory.newDefaultInstance()
                                        .newDuration(CdsMeteringSeries.resolution(usageSegment).toMillis());
        for (var entry : usageSegment.usageSegmentValues().entrySet()) {
            var format = entry.getKey();
            var direction = CdsMeteringSeries.direction(format);
            var points = new ArrayList<Point>();
            for (var point : CdsMeteringSeries.points(permissionRequest, usageSegment, entry.getValue(), format)) {
                points.add(new Point()
                                   .withPosition(point.position())
                                   .withEnergyQuantityQuantity(point.quantity())
                                   .withEnergyQuantityQuality(point.quality()));
            }
            var timeSeries = new TimeSeries()
                    .withVersion("1")
                    .withMRID(UUID.randomUUID().toString())
                    .withBusinessType(CdsMeteringSeries.businessType(direction))
                    .withProduct(CimCodes.EnergyProduct.ACTIVE_POWER)
                    .withFlowDirectionDirection(direction)
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind.fromValue(CimCodes.Aggregate.SUM))
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.fromValue(CdsMeteringSeries.commodity(format)))
                    .withEnergyMeasurementUnitName(CdsMeteringSeries.unit(format))
                    .withMarketEvaluationPointMRID(
                            new MeasurementPointIDString()
                                    .withCodingScheme(getCodingScheme())
                                    .withValue(meter.meterNumber())
                    )
                    .withReasonCode(CimCodes.ReasonCode.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
                    .withPeriods(
                            new SeriesPeriod()
                                    .withResolution(resolution)
                                    .withTimeInterval(
                                            new ESMPDateTimeInterval()
                                                    .withStart(interval.start())
                                                    .withEnd(interval.end())
                                    )
                                    .withPoints(points)
                    );
            timeSeriesList.add(timeSeries);
        }
        return timeSeriesList;
    }

    @Nullable
    private String getCodingScheme() {
        var countryCode = permissionRequest.dataSourceInformation().countryCode();
        return CimUtils.getCodingSchemeVhdV104(countryCode);
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
