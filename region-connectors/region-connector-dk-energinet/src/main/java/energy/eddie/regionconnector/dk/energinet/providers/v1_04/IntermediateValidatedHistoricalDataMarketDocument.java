// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.cim.v1_04.vhd.Point;
import energy.eddie.cim.v1_04.vhd.TimeSeries;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.EnerginetMeteringSeries;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;

class IntermediateValidatedHistoricalDataMarketDocument {
    private final CommonInformationModelConfiguration cimConfig;
    private final MyEnergyDataMarketDocument myEnergyDataMarketDocument;
    private final DkEnerginetPermissionRequest permissionRequest;

    public IntermediateValidatedHistoricalDataMarketDocument(
            CommonInformationModelConfiguration cimConfig,
            IdentifiableApiResponse identifiableApiResponse
    ) {
        this.cimConfig = cimConfig;
        this.myEnergyDataMarketDocument = identifiableApiResponse.payload().getMyEnergyDataMarketDocument();
        this.permissionRequest = identifiableApiResponse.permissionRequest();
    }

    public VHDEnvelope value() {
        var timeSeriesList = Optional.ofNullable(myEnergyDataMarketDocument.getTimeSeries()).orElse(List.of());
        var createdDateTime = myEnergyDataMarketDocument.getCreatedDateTime();
        var created = createdDateTime == null
                ? ZonedDateTime.now(DK_ZONE_ID)
                : ZonedDateTime.parse(createdDateTime);
        var epCodingScheme = cimConfig.eligiblePartyNationalCodingScheme().value();
        var vhd = new VHDMarketDocument()
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withMRID(Optional.ofNullable(myEnergyDataMarketDocument.getmRID())
                                  .orElse(UUID.randomUUID().toString()))
                .withCreatedDateTime(created)
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withValue(EnerginetRegionConnectorMetadata.GLOBAL_LOCATION_NUMBER)
                                .withCodingScheme(StandardCodingSchemeTypeList.GS1.value())
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withValue(cimConfig.eligiblePartyFallbackId())
                                .withCodingScheme(getStandardCodingSchemeTypeList(epCodingScheme).value())
                )
                .withPeriodTimeInterval(fromPeriodTimeInterval(myEnergyDataMarketDocument.getPeriodTimeInterval()))
                .withTimeSeries(timeSeries(timeSeriesList));
        return new VhdEnvelopeWrapper(vhd, permissionRequest).wrap();
    }

    private static List<SeriesPeriod> getPeriods(List<Period> periodList) {
        var periods = new ArrayList<SeriesPeriod>();
        for (var period : EnerginetMeteringSeries.periods(periodList, code -> isKnown(code, StandardQualityTypeList::fromValue))) {
            var points = new ArrayList<Point>(period.points().size());
            for (var point : period.points()) {
                points.add(new Point()
                                   .withPosition(point.position())
                                   .withEnergyQuantityQuality(point.quality())
                                   .withEnergyQuantityQuantity(point.quantity()));
            }
            var timeInterval = period.start() == null
                    ? new ESMPDateTimeInterval()
                    : new ESMPDateTimeInterval().withStart(period.start()).withEnd(period.end());
            periods.add(new SeriesPeriod()
                                .withResolution(period.resolution() == null
                                                        ? null
                                                        : DatatypeFactory.newDefaultInstance().newDuration(period.resolution()))
                                .withTimeInterval(timeInterval)
                                .withPoints(points)
                                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value()));
        }
        return periods;
    }

    private ESMPDateTimeInterval fromPeriodTimeInterval(@Nullable PeriodtimeInterval periodTimeInterval) {
        if (periodTimeInterval == null) {
            return new ESMPDateTimeInterval();
        }
        var interval = new EsmpTimeInterval(periodTimeInterval.getStart(), periodTimeInterval.getEnd());
        return new ESMPDateTimeInterval()
                .withStart(interval.start())
                .withEnd(interval.end());
    }

    private List<TimeSeries> timeSeries(List<energy.eddie.regionconnector.dk.energinet.customer.model.TimeSeries> timeSeriesList) {
        var result = new ArrayList<TimeSeries>();
        for (var timeSeries : timeSeriesList) {
            var businessType = EnerginetMeteringSeries.businessType(
                    timeSeries.getBusinessType(),
                    code -> isKnown(code, StandardBusinessTypeList::fromValue)
            );
            var marketEvaluationPoint = Optional.ofNullable(timeSeries.getMarketEvaluationPoint())
                                                .map(MarketEvaluationPoint::getmRID);
            List<Period> periodList = timeSeries.getPeriod() == null ? List.of() : timeSeries.getPeriod();

            var codingScheme = getStandardCodingSchemeTypeList(
                    marketEvaluationPoint.map(MarketEvaluationMeteringPoint::getCodingScheme)
                                         .orElse(null)
            );
            var ts = new TimeSeries()
                    .withMRID(timeSeries.getmRID())
                    .withBusinessType(businessType)
                    .withVersion("1")
                    .withProduct(StandardEnergyProductTypeList.ACTIVE_POWER.value())
                    .withFlowDirectionDirection(EnerginetMeteringSeries.direction(businessType))
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                    .withEnergyMeasurementUnitName(getStandardUnitOfMeasureTypeList(timeSeries).value())
                    .withMarketEvaluationPointMRID(
                            new MeasurementPointIDString()
                                    .withValue(marketEvaluationPoint.map(MarketEvaluationMeteringPoint::getName)
                                                                    .orElse(""))
                                    .withCodingScheme(codingScheme.value())
                    )
                    .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                    .withPeriods(getPeriods(periodList));

            result.add(ts);
        }
        return result;
    }

    private static StandardUnitOfMeasureTypeList getStandardUnitOfMeasureTypeList(energy.eddie.regionconnector.dk.energinet.customer.model.TimeSeries timeSeries) {
        return toEnum(timeSeries.getMeasurementUnitName(),
                      StandardUnitOfMeasureTypeList::fromValue,
                      StandardUnitOfMeasureTypeList.ONE);
    }

    private static StandardCodingSchemeTypeList getStandardCodingSchemeTypeList(@Nullable String codingScheme) {
        return toEnum(codingScheme, StandardCodingSchemeTypeList::fromValue, StandardCodingSchemeTypeList.GS1);
    }

    private static boolean isKnown(String code, Function<String, ?> converter) {
        try {
            return converter.apply(code) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static <T> T toEnum(@Nullable String value, Function<String, T> converter, T defaultValue) {
        try {
            return converter.apply(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
