// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_12;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_12.*;
import energy.eddie.cim.v1_12.vhd.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.providers.cim.Account;
import energy.eddie.regionconnector.cds.providers.cim.Meter;
import energy.eddie.regionconnector.cds.providers.cim.UsageSegment;
import energy.eddie.regionconnector.cds.providers.vhd.CdsMeteringSeries;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import energy.eddie.regionconnector.shared.cim.v0_82.CimUtils;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_12.vhd.VhdEnvelopeWrapper;
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
                    // dateAndOrTime.dateTime is mandatory in CIM v1.12, use the creation time of the document
                    .withDateAndOrTimeDateTime(created)
                    .withBusinessType(CdsMeteringSeries.businessType(direction))
                    .withProduct(CimCodes.EnergyProduct.ACTIVE_POWER)
                    .withFlowDirectionDirection(direction)
                    .withAccountingPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind.fromValue(CimCodes.Aggregate.SUM))
                    .withAccountingPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.fromValue(CdsMeteringSeries.commodity(format)))
                    .withEnergyMeasurementUnitName(CdsMeteringSeries.unit(format))
                    .withAccountingPointMRID(
                            new MeasurementPointIDString()
                                    .withCodingScheme(getCodingScheme())
                                    .withValue(meter.meterNumber())
                    )
                    .withReasonCode(CimCodes.ReasonCode.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
                    .withSeriesPeriods(
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
