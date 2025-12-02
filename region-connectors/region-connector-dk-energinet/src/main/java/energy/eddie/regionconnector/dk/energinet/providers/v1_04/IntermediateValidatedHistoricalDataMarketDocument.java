package energy.eddie.regionconnector.dk.energinet.providers.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.cim.v1_04.vhd.Point;
import energy.eddie.cim.v1_04.vhd.TimeSeries;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.filter.EnerginetResolution;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.math.BigDecimal;
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

    public List<SeriesPeriod> getPeriods(List<Period> periodList) {
        var periods = new ArrayList<SeriesPeriod>();
        for (var period : periodList) {
            var pointList = Optional.ofNullable(period.getPoint()).orElse(List.of());
            var duration = parseResolution(period.getResolution());
            var seriesPeriod = new SeriesPeriod()
                    .withResolution(duration)
                    .withTimeInterval(fromPeriodTimeInterval(period.getTimeInterval()))
                    .withPoints(fromPointList(pointList))
                    .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value());
            periods.add(seriesPeriod);
        }
        return periods;
    }

    @Nullable
    private static Duration parseResolution(@Nullable String resolution) {
        if (resolution == null) {
            return null;
        }
        return DatatypeFactory.newDefaultInstance()
                              .newDuration(new EnerginetResolution(resolution).toISO8601Duration());
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
            var businessType = getBusinessType(timeSeries);
            var marketEvaluationPoint = Optional.ofNullable(timeSeries.getMarketEvaluationPoint())
                                                .map(MarketEvaluationPoint::getmRID);
            List<Period> periodList = timeSeries.getPeriod() == null ? List.of() : timeSeries.getPeriod();

            var codingScheme = getStandardCodingSchemeTypeList(
                    marketEvaluationPoint.map(MarketEvaluationMeteringPoint::getCodingScheme)
                                         .orElse(null)
            );
            var ts = new TimeSeries()
                    .withMRID(timeSeries.getmRID())
                    .withBusinessType(businessType == null ? null : businessType.value())
                    .withVersion("1")
                    .withProduct(StandardEnergyProductTypeList.ACTIVE_POWER.value())
                    .withFlowDirectionDirection(getDirection(businessType).value())
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

    private static StandardDirectionTypeList getDirection(@Nullable StandardBusinessTypeList businessType) {
        return switch (businessType) {
            case CONSUMPTION -> StandardDirectionTypeList.DOWN;
            case PRODUCTION -> StandardDirectionTypeList.UP;
            case null, default -> StandardDirectionTypeList.UP_AND_DOWN;
        };
    }

    @Nullable
    @SuppressWarnings("NullAway")
    private static StandardBusinessTypeList getBusinessType(energy.eddie.regionconnector.dk.energinet.customer.model.TimeSeries timeSeries) {
        return toEnum(timeSeries.getBusinessType(), StandardBusinessTypeList::fromValue, null);
    }

    private List<Point> fromPointList(List<energy.eddie.regionconnector.dk.energinet.customer.model.Point> pointList) {
        var list = new ArrayList<Point>();
        var i = 1;
        for (var point : pointList) {
            var outQuantityQuantity = point.getOutQuantityQuantity() == null
                    ? 0
                    : Double.parseDouble(point.getOutQuantityQuantity());
            var position = point.getPosition();
            list.add(
                    new Point()
                            .withPosition(position == null ? i : Integer.parseInt(position))
                            .withEnergyQuantityQuality(getStandardQualityTypeList(point).value())
                            .withEnergyQuantityQuantity(BigDecimal.valueOf(outQuantityQuantity))
            );
            i++;
        }
        return list;
    }

    private static StandardQualityTypeList getStandardQualityTypeList(energy.eddie.regionconnector.dk.energinet.customer.model.Point point) {
        return toEnum(point.getOutQuantityQuality(),
                      StandardQualityTypeList::fromValue,
                      StandardQualityTypeList.AS_PROVIDED);
    }

    private static <T> T toEnum(@Nullable String value, Function<String, T> converter, T defaultValue) {
        try {
            return converter.apply(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
