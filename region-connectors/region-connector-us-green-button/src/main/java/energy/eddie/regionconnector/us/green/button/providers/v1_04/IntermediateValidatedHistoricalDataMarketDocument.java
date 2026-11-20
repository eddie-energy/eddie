// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v1_04;

import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.shared.cim.CimCodes;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import energy.eddie.regionconnector.us.green.button.atom.feed.Query;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.providers.GreenButtonMeteringSeries;
import energy.eddie.regionconnector.us.green.button.providers.MeasurementScale;
import energy.eddie.regionconnector.us.green.button.providers.ReadingTypeValueConverter;
import energy.eddie.regionconnector.us.green.button.providers.UnsupportedUnitException;
import jakarta.xml.bind.Unmarshaller;
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
        var intervalReadings = GreenButtonMeteringSeries.intervalReadings(query, permissionId);
        var vhds = new ArrayList<VHDEnvelope>(intervalReadings.size());
        for (var readings : intervalReadings) {
            vhds.add(new VhdEnvelopeWrapper(getVHDMarketDocument(readings), permissionRequest).wrap());
        }
        return vhds;
    }

    private VHDMarketDocument getVHDMarketDocument(
            GreenButtonMeteringSeries.IntervalReadings intervalReadings
    ) throws UnsupportedUnitException {
        var readingType = intervalReadings.readingType();
        var interval = intervalReadings.intervalBlock().getInterval();
        var start = Instant.ofEpochSecond(interval.getStart()).atZone(ZoneOffset.UTC);
        var end = Instant.ofEpochSecond(interval.getStart() + interval.getDuration()).atZone(ZoneOffset.UTC);
        var flowDirection = GreenButtonMeteringSeries.direction(readingType);
        var commodity = GreenButtonMeteringSeries.commodity(readingType);
        var scale = ReadingTypeValueConverter.v104UnitOfMeasureTypeList(readingType).scale();
        var permissionAdmin = permissionRequest.dataSourceInformation().permissionAdministratorId();
        var clientId = greenButtonConfiguration.clientIds().getOrDefault(permissionAdmin, "");
        var meterUid = Query.intervalBlockSelfToUsagePointId(intervalReadings.intervalBlockEntry());

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
                                .withStart(start.toString())
                                .withEnd(end.toString())
                )
                .withTimeSeries(
                        new TimeSeries()
                                .withVersion("1")
                                .withMRID(UUID.randomUUID().toString())
                                .withBusinessType(Objects.requireNonNull(GreenButtonMeteringSeries.businessType(flowDirection)))
                                .withProduct(CimCodes.EnergyProduct.ACTIVE_POWER)
                                .withFlowDirectionDirection(Objects.requireNonNull(flowDirection))
                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind.fromValue(CimCodes.Aggregate.SUM))
                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(
                                        commodity == null ? null : CommodityKind.fromValue(commodity)
                                )
                                .withEnergyMeasurementUnitName(scale.unit().value())
                                .withMarketEvaluationPointMRID(
                                        new MeasurementPointIDString()
                                                .withCodingScheme(StandardCodingSchemeTypeList.CGM.value())
                                                .withValue(meterUid)
                                )
                                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                                .withPeriods(getSeriesPeriods(intervalReadings, scale))
                );
    }

    private static List<SeriesPeriod> getSeriesPeriods(
            GreenButtonMeteringSeries.IntervalReadings intervalReadings,
            MeasurementScale<StandardUnitOfMeasureTypeList> scale
    ) {
        List<SeriesPeriod> seriesPeriods = new ArrayList<>();
        for (var period : GreenButtonMeteringSeries.periods(intervalReadings, scale)) {
            seriesPeriods.add(new SeriesPeriod()
                                      .withResolution(DatatypeFactory.newDefaultInstance()
                                                                     .newDuration(period.resolution().toMillis()))
                                      .withTimeInterval(new ESMPDateTimeInterval()
                                                                .withStart(period.start())
                                                                .withEnd(period.end()))
                                      .withPoints(new Point()
                                                          .withPosition(1)
                                                          .withEnergyQuantityQuantity(period.value())
                                                          .withEnergyQuantityQuality(CimCodes.Quality.AS_PROVIDED)));
        }
        return seriesPeriods;
    }
}
