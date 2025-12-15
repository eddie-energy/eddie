package energy.eddie.regionconnector.us.green.button.providers.v1_04;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import energy.eddie.regionconnector.us.green.button.atom.feed.Query;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.providers.MeasurementScale;
import energy.eddie.regionconnector.us.green.button.providers.ReadingTypeValueConverter;
import energy.eddie.regionconnector.us.green.button.providers.UnsupportedUnitException;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.Unmarshaller;
import org.naesb.espi.IntervalBlock;
import org.naesb.espi.ReadingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import javax.xml.datatype.DatatypeFactory;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static energy.eddie.regionconnector.us.green.button.GreenButtonRegionConnectorMetadata.US_ZONE_ID;

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

    public List<VHDEnvelope> toVhd() throws UnsupportedUnitException {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Mapping validated historical data of permission request {} to CIM", permissionId);
        var query = new Query(syndFeed, unmarshaller);
        var readings = query.findAllByTitle("ReadingType");
        var vhds = new ArrayList<VHDEnvelope>(readings.size());
        for (var reading : readings) {
            var related = reading.findRelatedLink("related").getHref();
            var optionalIntervalBlock = query.findFirstBySelfLinkAndTitle(related, "IntervalBlock");
            var readingType = query.unmarshal(reading, ReadingType.class);

            if (optionalIntervalBlock.isEmpty() || readingType == null) {
                LOGGER.warn("Could not parse interval block for reading type {} for permission request {}",
                        related,
                        permissionId);
                continue;
            }

            var optionalIntervalBlockVal = optionalIntervalBlock.get();
            var intervalBlock = query.unmarshal(optionalIntervalBlockVal, IntervalBlock.class);
            if (intervalBlock == null) {
                LOGGER.warn("Could not parse interval block {} for permission request {}", related, permissionId);
            } else {
                var vhd = getVHDMarketDocument(readingType, intervalBlock, optionalIntervalBlockVal);
                vhds.add(new VhdEnvelopeWrapper(vhd, permissionRequest).wrap());
            }
        }
        return vhds;
    }

    private VHDMarketDocument getVHDMarketDocument(ReadingType readingType,
                                                   IntervalBlock intervalBlock,
                                                   SyndEntry optionalIntervalBlockVal) throws UnsupportedUnitException {
        var interval = intervalBlock.getInterval();
        var start = Instant.ofEpochSecond(interval.getStart()).atZone(ZoneOffset.UTC);
        var end = Instant.ofEpochSecond(interval.getStart() + interval.getDuration()).atZone(ZoneOffset.UTC);
        var esmpInterval = new ESMPDateTimeInterval()
                .withStart(start.toString())
                .withEnd(end.toString());
        var flowDirection = getDirection(readingType.getFlowDirection());
        var scale = ReadingTypeValueConverter.v104UnitOfMeasureTypeList(readingType).scale();
        var seriesPeriods = getSeriesPeriods(readingType, intervalBlock, scale);
        var permissionAdmin = permissionRequest.dataSourceInformation().permissionAdministratorId();
        var clientId = greenButtonConfiguration.clientIds().getOrDefault(permissionAdmin, "");
        var meterUid = Query.intervalBlockSelfToUsagePointId(optionalIntervalBlockVal);

        return new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withCreatedDateTime(ZonedDateTime.now(US_ZONE_ID))
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.CGM.value())
                                .withValue(permissionAdmin)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme().value())
                                .withValue(clientId)
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(esmpInterval.getStart())
                                .withEnd(esmpInterval.getEnd())
                )
                .withTimeSeries(
                        new TimeSeries()
                                .withVersion("1")
                                .withMRID(UUID.randomUUID().toString())
                                .withBusinessType(Objects.requireNonNull(getBusinessType(flowDirection)).value())
                                .withProduct(StandardEnergyProductTypeList.ACTIVE_POWER.value())
                                .withFlowDirectionDirection(Objects.requireNonNull(flowDirection).value())
                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(
                                        AggregateKind.SUM
                                )
                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(
                                        getCommodity(readingType)
                                )
                                .withEnergyMeasurementUnitName(scale.unit().value())
                                .withMarketEvaluationPointMRID(
                                        new MeasurementPointIDString()
                                                .withCodingScheme(StandardCodingSchemeTypeList.CGM.value())
                                                .withValue(meterUid)
                                )
                                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                                .withPeriods(seriesPeriods)
                );
    }

    private List<SeriesPeriod> getSeriesPeriods(ReadingType readingType,
                                                IntervalBlock intervalBlock,
                                                MeasurementScale<StandardUnitOfMeasureTypeList> scale) {
        List<SeriesPeriod> seriesPeriods = new ArrayList<>();
        for (var intervalReading : intervalBlock.getIntervalReading()) {
            var timePeriod = intervalReading.getTimePeriod();
            var interval = new ESMPDateTimeInterval()
                    .withStart(Instant.ofEpochSecond(
                            timePeriod.getStart()
                    ).atZone(ZoneOffset.UTC).toString())
                    .withEnd(Instant.ofEpochSecond(
                            timePeriod.getStart() + timePeriod.getDuration()
                    ).atZone(ZoneOffset.UTC).toString());

            var granularity = Granularity.fromMinutes(Math.toIntExact(readingType.getIntervalLength() / 60));
            var duration = DatatypeFactory.newDefaultInstance()
                    .newDuration(granularity.duration().toMillis());
            var value = scale.scaled(intervalReading.getValue());
            var seriesPeriod = new SeriesPeriod()
                    .withResolution(duration)
                    .withTimeInterval(
                            new ESMPDateTimeInterval()
                                    .withStart(interval.getStart())
                                    .withEnd(interval.getEnd())
                    )
                    .withPoints(
                            new Point()
                                    .withPosition(1)
                                    .withEnergyQuantityQuantity(value)
                                    .withEnergyQuantityQuality(StandardQualityTypeList.AS_PROVIDED.value())
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
    private static StandardDirectionTypeList getDirection(String direction) {
        return switch (direction) {
            case "1" -> StandardDirectionTypeList.DOWN;
            case "19" -> StandardDirectionTypeList.UP;
            case "4" -> StandardDirectionTypeList.UP_AND_DOWN;
            default -> null;
        };
    }

    @Nullable
    private static StandardBusinessTypeList getBusinessType(@Nullable StandardDirectionTypeList direction) {
        return switch (direction) {
            case UP -> StandardBusinessTypeList.PRODUCTION;
            case DOWN -> StandardBusinessTypeList.CONSUMPTION;
            case UP_AND_DOWN -> StandardBusinessTypeList.NET_PRODUCTION__CONSUMPTION;
            case null, default -> null;
        };
    }
}
