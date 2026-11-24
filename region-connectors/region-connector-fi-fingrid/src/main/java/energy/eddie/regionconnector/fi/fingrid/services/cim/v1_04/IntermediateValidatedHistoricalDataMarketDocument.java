// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeries;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.services.cim.FingridMeteringSeries;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;

import javax.xml.datatype.DatatypeFactory;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class IntermediateValidatedHistoricalDataMarketDocument {
    private final List<TimeSeriesResponse> timeSeriesResponses;
    private final FingridPermissionRequest permissionRequest;

    IntermediateValidatedHistoricalDataMarketDocument(
            List<TimeSeriesResponse> timeSeriesResponses,
            FingridPermissionRequest permissionRequest
    ) {
        this.timeSeriesResponses = timeSeriesResponses;
        this.permissionRequest = permissionRequest;
    }

    public List<VHDEnvelope> toVhds() {
        List<VHDEnvelope> vhds = new ArrayList<>();
        for (var timeSeriesResponse : timeSeriesResponses) {
            vhds.add(new VhdEnvelopeWrapper(toSingleVhd(timeSeriesResponse), permissionRequest).wrap());
        }
        return vhds;
    }

    private VHDMarketDocument toSingleVhd(TimeSeriesResponse timeSeriesResponse) {
        var data = timeSeriesResponse.data();
        var timeSeries = data.transaction().timeSeries();

        String meteringPointEAN = null;
        String unitOfMeasurement = null;
        List<SeriesPeriod> seriesPeriods = null;
        ESMPDateTimeInterval interval = null;

        if (timeSeries != null && !timeSeries.isEmpty()) {
            meteringPointEAN = timeSeries.getFirst().meteringPointEAN();
            unitOfMeasurement = StandardUnitOfMeasureTypeList.fromValue(timeSeries.getFirst()
                                                                                  .unitType()
                                                                                  .toUpperCase(Locale.ROOT)).value();
            seriesPeriods = getSeriesPeriods(timeSeries);
            interval = getOverallTimeInterval(timeSeries);
        }
        return new VHDMarketDocument()
                .withMRID(data.header().identification())
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withCreatedDateTime(data.header().creation())
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME.value())
                                .withValue(data.header().juridicalSenderParty().identification())
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME.value())
                                .withValue(data.header().juridicalReceiverParty().identification())
                )
                .withPeriodTimeInterval(interval)
                .withTimeSeries(
                        new energy.eddie.cim.v1_04.vhd.TimeSeries()
                                .withMRID(UUID.randomUUID().toString())
                                .withVersion("1")
                                .withProduct(StandardEnergyProductTypeList.ACTIVE_ENERGY.value())
                                .withFlowDirectionDirection(StandardDirectionTypeList.UP_AND_DOWN.value())
                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(
                                        CommodityKind.ELECTRICITYPRIMARYMETERED
                                )
                                .withEnergyMeasurementUnitName(unitOfMeasurement)
                                .withMarketEvaluationPointMRID(
                                        new MeasurementPointIDString()
                                                .withCodingScheme(StandardCodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME.value())
                                                .withValue(meteringPointEAN)
                                )
                                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                                .withReasonText(FingridMeteringSeries.reasons(data))
                                .withPeriods(seriesPeriods)
                );
    }

    private List<SeriesPeriod> getSeriesPeriods(List<TimeSeries> timeSeries) {
        var list = new ArrayList<SeriesPeriod>();
        for (var period : FingridMeteringSeries.periods(timeSeries)) {
            var points = new ArrayList<Point>(period.points().size());
            for (var point : period.points()) {
                points.add(new Point()
                                   .withPosition(point.position())
                                   .withEnergyQualityQuantityQuantity(point.quantity())
                                   .withEnergyQualityQuantityQuality(point.quality()));
            }
            list.add(
                    new SeriesPeriod()
                            .withResolution(DatatypeFactory.newDefaultInstance()
                                                           .newDuration(period.resolution().toMillis()))
                            .withTimeInterval(new ESMPDateTimeInterval()
                                                      .withStart(period.start())
                                                      .withEnd(period.end()))
                            .withPoints(points)
            );
        }
        return list;
    }

    private ESMPDateTimeInterval getOverallTimeInterval(List<TimeSeries> timeSeriesList) {
        ZonedDateTime start = null;
        ZonedDateTime end = null;
        for (var timeSeries : timeSeriesList) {
            if (start == null || start.isAfter(timeSeries.start())) {
                start = timeSeries.start();
            }
            if (end == null || end.isBefore(timeSeries.end())) {
                end = timeSeries.end();
            }
        }
        var interval = new EsmpTimeInterval(start, end);
        return new ESMPDateTimeInterval()
                .withStart(interval.start())
                .withEnd(interval.end());
    }
}
