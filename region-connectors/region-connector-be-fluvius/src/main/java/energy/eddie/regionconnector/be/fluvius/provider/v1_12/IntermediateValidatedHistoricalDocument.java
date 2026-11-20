// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_12;

import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_12.*;
import energy.eddie.cim.v1_12.vhd.*;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.provider.FluviusMeteringSeries;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_12.vhd.VhdEnvelopeWrapper;
import org.jspecify.annotations.Nullable;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class IntermediateValidatedHistoricalDocument {
    private final FluviusOAuthConfiguration fluviusConfig;
    private final IdentifiableMeteringData identifiableMeteredData;

    IntermediateValidatedHistoricalDocument(
            FluviusOAuthConfiguration fluviusConfiguration,
            IdentifiableMeteringData identifiableMeteredData
    ) {
        this.fluviusConfig = fluviusConfiguration;
        this.identifiableMeteredData = identifiableMeteredData;
    }

    public @Nullable Envelope toVHD() {
        GetEnergyResponseModelApiDataResponse meteringData = identifiableMeteredData.payload();
        if (meteringData.data().headpoint() == null) {
            return null;
        }
        return new VhdEnvelopeWrapper(createVHD(meteringData), identifiableMeteredData.permissionRequest()).wrap();
    }

    private @Nullable VHDMarketDocument createVHD(
            GetEnergyResponseModelApiDataResponse getEnergyResponseModel
    ) {
        var fetchTime = getEnergyResponseModel.metaData().fetchTime() == null
                ? ZonedDateTime.now(ZoneOffset.UTC)
                : getEnergyResponseModel.metaData().fetchTime();
        var headpoint = getEnergyResponseModel.data().headpoint();
        if (headpoint == null) {
            return null;
        }
        var timeSeriesList = createTimeSeriesList(headpoint);
        if (timeSeriesList.isEmpty()) {
            return null;
        }
        // dateAndOrTime.dateTime is mandatory in CIM v1.12, use the creation time of the document
        for (var timeSeries : timeSeriesList) {
            timeSeries.setDateAndOrTimeDateTime(fetchTime);
        }
        return new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V1_12.cimify())
                .withType(StandardDocumentTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withCreatedDateTime(fetchTime)
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME.value())
                                .withValue("Fluvius")
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME.value())
                                .withValue(fluviusConfig.clientId())
                )
                .withTimeSeries(timeSeriesList);
    }

    private List<TimeSeries> createTimeSeriesList(Headpoint headpoint) {
        var granularity = identifiableMeteredData.permissionRequest().granularity();
        var tss = new ArrayList<TimeSeries>();
        for (var series : FluviusMeteringSeries.of(headpoint, granularity)) {
            tss.add(new TimeSeries()
                            .withMRID(UUID.randomUUID().toString())
                            .withBusinessType(series.businessType())
                            .withProduct(series.product())
                            .withFlowDirectionDirection(series.direction())
                            .withVersion("1")
                            .withAccountingPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.fromValue(series.commodity()))
                            .withEnergyMeasurementUnitName(series.unit())
                            .withAccountingPointMRID(new MeasurementPointIDString()
                                                   .withCodingScheme(StandardCodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME.value())
                                                   .withValue(series.ean()))
                            .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                            .withSeriesPeriods(seriesPeriod(series)));
        }
        return tss;
    }

    private SeriesPeriod seriesPeriod(FluviusMeteringSeries.Series series) {
        var points = new ArrayList<Point>(series.points().size());
        for (var point : series.points()) {
            points.add(new Point()
                               .withPosition(point.position())
                               .withEnergyQuantityQuantity(point.quantity())
                               .withEnergyQuantityQuality(point.quality()));
        }
        var interval = new EsmpTimeInterval(series.start(), series.end());
        return new SeriesPeriod()
                .withResolution(getResolution())
                .withTimeInterval(new ESMPDateTimeInterval()
                                          .withStart(interval.start())
                                          .withEnd(interval.end()))
                .withPoints(points)
                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value());
    }

    private Duration getResolution() {
        return DatatypeFactory.newDefaultInstance()
                              .newDuration(identifiableMeteredData.permissionRequest()
                                                                  .granularity()
                                                                  .duration()
                                                                  .toMillis());
    }
}
