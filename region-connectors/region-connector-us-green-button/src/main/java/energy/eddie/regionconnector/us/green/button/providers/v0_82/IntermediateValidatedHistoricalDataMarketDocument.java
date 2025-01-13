package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.us.green.button.atom.feed.Query;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.Unmarshaller;
import org.naesb.espi.IntervalBlock;
import org.naesb.espi.ReadingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class IntermediateValidatedHistoricalDataMarketDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateValidatedHistoricalDataMarketDocument.class);
    private final UsGreenButtonPermissionRequest permissionRequest;
    private final SyndFeed syndFeed;
    private final Unmarshaller unmarshaller;
    private final CommonInformationModelConfiguration cimConfig;
    private final GreenButtonConfiguration greenButtonConfiguration;

    public IntermediateValidatedHistoricalDataMarketDocument(
            IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> identifiableSyndFeed,
            Jaxb2Marshaller marshaller,
            CommonInformationModelConfiguration cimConfig,
            GreenButtonConfiguration greenButtonConfiguration
    ) {
        permissionRequest = identifiableSyndFeed.permissionRequest();
        syndFeed = identifiableSyndFeed.payload();
        unmarshaller = marshaller.createUnmarshaller();
        this.cimConfig = cimConfig;
        this.greenButtonConfiguration = greenButtonConfiguration;
    }

    @SuppressWarnings("java:S135")
    public List<ValidatedHistoricalDataEnvelope> toVhd() throws UnsupportedUnitException {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Mapping validated historical data of permission request {} to CIM", permissionId);
        var query = new Query(syndFeed, unmarshaller);
        var now = EsmpDateTime.now();
        var permissionAdmin = permissionRequest.dataSourceInformation().permissionAdministratorId();
        var clientId = greenButtonConfiguration.clientIds().getOrDefault(permissionAdmin, "");
        var readings = query.findAllByTitle("ReadingType");
        var vhds = new ArrayList<ValidatedHistoricalDataEnvelope>(readings.size());
        for (var reading : readings) {
            var related = reading.findRelatedLink("related").getHref();
            var optionalIntervalBlock = query.findFirstBySelfLinkAndTitle(related, "IntervalBlock");
            var readingType = query.unmarshal(reading, ReadingType.class);
            if (optionalIntervalBlock.isEmpty() || readingType == null) {
                LOGGER.warn("Could not parse interval block or reading type {} for permission request {}",
                            related,
                            permissionId);
                continue;
            }
            var meterUid = Query.intervalBlockSelfToUsagePointId(optionalIntervalBlock.get());
            var intervalBlock = query.unmarshal(optionalIntervalBlock.get(), IntervalBlock.class);
            if (intervalBlock == null) {
                LOGGER.warn("Could not parse interval block {} for permission request {}", related, permissionId);
                continue;
            }
            var interval = intervalBlock.getInterval();
            var start = Instant.ofEpochSecond(interval.getStart()).atZone(ZoneOffset.UTC);
            var end = Instant.ofEpochSecond(interval.getStart() + interval.getDuration()).atZone(ZoneOffset.UTC);
            var esmpInterval = new EsmpTimeInterval(start, end);
            var flowDirection = getDirection(readingType.getFlowDirection());
            var scale = new ReadingTypeValueConverter(readingType).scale();
            var seriesPeriods = getSeriesPeriods(readingType, intervalBlock, scale);
            var vhd = new ValidatedHistoricalDataMarketDocumentComplexType()
                    .withMRID(UUID.randomUUID().toString())
                    .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                    .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
                    .withCreatedDateTime(now.toString())
                    .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                    .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                    .withProcessProcessType(ProcessTypeList.REALISED)
                    .withSenderMarketParticipantMRID(
                            new PartyIDStringComplexType()
                                    .withCodingScheme(CodingSchemeTypeList.CGM)
                                    .withValue(permissionAdmin)
                    )
                    .withReceiverMarketParticipantMRID(
                            new PartyIDStringComplexType()
                                    .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme())
                                    .withValue(clientId)
                    )
                    .withPeriodTimeInterval(
                            new ESMPDateTimeIntervalComplexType()
                                    .withStart(esmpInterval.start())
                                    .withEnd(esmpInterval.end())
                    )
                    .withTimeSeriesList(
                            new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                    .withTimeSeries(
                                            new TimeSeriesComplexType()
                                                    .withMRID(UUID.randomUUID().toString())
                                                    .withBusinessType(getBusinessType(flowDirection))
                                                    .withProduct(EnergyProductTypeList.ACTIVE_POWER)
                                                    .withFlowDirectionDirection(flowDirection)
                                                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(
                                                            AggregateKind.SUM
                                                    )
                                                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(
                                                            getCommodity(readingType)
                                                    )
                                                    .withEnergyMeasurementUnitName(scale.unit())
                                                    .withMarketEvaluationPointMRID(
                                                            new MeasurementPointIDStringComplexType()
                                                                    .withCodingScheme(CodingSchemeTypeList.CGM)
                                                                    .withValue(meterUid)
                                                    )
                                                    .withReasonList(
                                                            new TimeSeriesComplexType.ReasonList()
                                                                    .withReasons(
                                                                            new ReasonComplexType()
                                                                                    .withCode(
                                                                                            ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED
                                                                                    )
                                                                    )
                                                    )
                                                    .withSeriesPeriodList(
                                                            new TimeSeriesComplexType.SeriesPeriodList()
                                                                    .withSeriesPeriods(seriesPeriods)
                                                    )
                                    )
                    );
            vhds.add(new VhdEnvelope(vhd, permissionRequest).wrap());
        }
        return vhds;
    }

    private List<SeriesPeriodComplexType> getSeriesPeriods(ReadingType readingType, IntervalBlock intervalBlock, MeasurementScale scale) {
        var granularity = Granularity.fromMinutes(Math.toIntExact(readingType.getIntervalLength() / 60));
        List<SeriesPeriodComplexType> seriesPeriods = new ArrayList<>();
        for (var intervalReading : intervalBlock.getIntervalReading()) {
            var timePeriod = intervalReading.getTimePeriod();
            var interval = new EsmpTimeInterval(
                    Instant.ofEpochSecond(timePeriod.getStart()).atZone(ZoneOffset.UTC),
                    Instant.ofEpochSecond(timePeriod.getStart() + timePeriod.getDuration()).atZone(ZoneOffset.UTC)
            );

            var value = scale.scaled(intervalReading.getValue());
            var seriesPeriod = new SeriesPeriodComplexType()
                    .withResolution(granularity.toString())
                    .withTimeInterval(
                            new ESMPDateTimeIntervalComplexType()
                                    .withStart(interval.start())
                                    .withEnd(interval.end())
                    )
                    .withPointList(
                            new SeriesPeriodComplexType.PointList()
                                    .withPoints(
                                            new PointComplexType()
                                                    .withPosition(String.valueOf(timePeriod.getStart()))
                                                    .withEnergyQuantityQuantity(value)
                                                    .withEnergyQuantityQuality(QualityTypeList.AS_PROVIDED)
                                    )
                    );
            seriesPeriods.add(seriesPeriod);
        }
        return seriesPeriods;
    }

    @Nullable
    private static CommodityKind getCommodity(ReadingType readingType) {
        return switch (readingType.getCommodity()) {
            case "2" -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case "1" -> CommodityKind.ELECTRICITYSECONDARYMETERED;
            default -> null;
        };
    }

    @Nullable
    private static DirectionTypeList getDirection(String direction) {
        return switch (direction) {
            case "1" -> DirectionTypeList.DOWN;
            case "19" -> DirectionTypeList.UP;
            case "4" -> DirectionTypeList.UP_AND_DOWN;
            default -> null;
        };
    }

    @Nullable
    private static BusinessTypeList getBusinessType(@Nullable DirectionTypeList direction) {
        return switch (direction) {
            case UP -> BusinessTypeList.PRODUCTION;
            case DOWN -> BusinessTypeList.CONSUMPTION;
            case UP_AND_DOWN -> BusinessTypeList.NET_PRODUCTION__CONSUMPTION;
            case null, default -> null;
        };
    }
}
