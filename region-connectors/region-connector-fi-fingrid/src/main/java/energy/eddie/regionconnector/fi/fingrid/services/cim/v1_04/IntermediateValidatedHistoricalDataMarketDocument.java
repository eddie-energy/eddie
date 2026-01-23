// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.fi.fingrid.client.model.Observation;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeries;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesData;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

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
                                .withReasonText(getReasons(data))
                                .withPeriods(seriesPeriods)
                );
    }

    private List<SeriesPeriod> getSeriesPeriods(List<TimeSeries> timeSeries) {
        var list = new ArrayList<SeriesPeriod>();
        for (var series : timeSeries) {
            var interval = new EsmpTimeInterval(series.start(), series.end());
            var points = getPoints(series);
            var duration = DatatypeFactory.newDefaultInstance()
                                          .newDuration(series.resolutionDuration().duration().toMillis());
            list.add(
                    new SeriesPeriod()
                            .withResolution(duration)
                            .withTimeInterval(new ESMPDateTimeInterval()
                                                      .withStart(interval.start())
                                                      .withEnd(interval.end()))
                            .withPoints(points)
            );
        }
        return list;
    }

    private static List<Point> getPoints(TimeSeries series) {
        List<Point> result = new ArrayList<>();
        var position = 1;
        for (Observation obs : series.observations()) {
            Point point = new Point()
                    .withPosition(position)
                    .withEnergyQualityQuantityQuantity(obs.quantity())
                    .withEnergyQualityQuantityQuality(
                            obs.quality()
                               .equals("OK") ? StandardQualityTypeList.AS_PROVIDED.value() : StandardQualityTypeList.NOT_AVAILABLE.value()
                    );
            result.add(point);
            position++;
        }
        return result;
    }

    @Nullable
    private static String getReasons(TimeSeriesData data) {
        var eventReasons = data.transaction().eventReasons();
        if (eventReasons == null || eventReasons.reasons().isEmpty()) {
            return null;
        }
        var reasons = new StringBuilder();
        for (var reason : eventReasons.reasons()) {
            reasons.append(reason.eventReasonText())
                   .append("\n");
        }
        return reasons.toString();
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
