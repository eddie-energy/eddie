package energy.eddie.regionconnector.de.eta.providers.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteringSeries;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;

import javax.xml.datatype.DatatypeFactory;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Maps a single ETA Plus historical-readings response (one metering point, one unit, one direction)
 * to a CIM v1.04 {@link VHDMarketDocument} wrapped in a {@link VHDEnvelope}.
 *
 * <p>Per the ETA Plus contract, {@code unit} and {@code direction} are constant within a response,
 * so each response yields exactly one {@link TimeSeries}.
 */
class IntermediateValidatedHistoricalDataMarketDocument {
    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration deConfiguration;
    private final IdentifiableValidatedHistoricalData identifiableData;

    IntermediateValidatedHistoricalDataMarketDocument(
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration,
            IdentifiableValidatedHistoricalData identifiableData
    ) {
        this.cimConfig = cimConfig;
        this.deConfiguration = deConfiguration;
        this.identifiableData = identifiableData;
    }

    public List<VHDEnvelope> toVhd() {
        var optionalSeries = EtaPlusMeteringSeries.of(identifiableData);
        if (optionalSeries.isEmpty()) {
            return List.of();
        }
        var series = optionalSeries.get();
        var permissionRequest = identifiableData.permissionRequest();
        var senderId = permissionRequest.dataSourceInformation().meteredDataAdministratorId();
        var vhd = new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withCreatedDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME.value())
                                .withValue(senderId)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme().value())
                                .withValue(deConfiguration.eligiblePartyId())
                )
                .withPeriodTimeInterval(esmpInterval(series.start(), series.end()))
                .withTimeSeries(timeSeries(series));

        return List.of(new VhdEnvelopeWrapper(vhd, permissionRequest).wrap());
    }

    private TimeSeries timeSeries(EtaPlusMeteringSeries.Series series) {
        var points = new ArrayList<Point>(series.points().size());
        for (var point : series.points()) {
            points.add(new Point()
                               .withPosition(point.position())
                               .withEnergyQuantityQuantity(point.quantity())
                               .withEnergyQuantityQuality(point.quality()));
        }
        var granularity = identifiableData.permissionRequest().granularity();
        return new TimeSeries()
                .withMRID(UUID.randomUUID().toString())
                .withVersion("1")
                .withBusinessType(series.businessType())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.fromValue(series.commodity()))
                .withFlowDirectionDirection(series.direction())
                .withEnergyMeasurementUnitName(series.unit())
                .withProduct(series.product())
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME.value())
                                .withValue(series.meteringPointId())
                )
                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                .withPeriods(
                        new SeriesPeriod()
                                .withResolution(DatatypeFactory.newDefaultInstance()
                                                               .newDuration(granularity.duration().toMillis()))
                                .withTimeInterval(esmpInterval(series.start(), series.end()))
                                .withPoints(points)
                );
    }

    private static ESMPDateTimeInterval esmpInterval(ZonedDateTime start, ZonedDateTime end) {
        var interval = new EsmpTimeInterval(start, end);
        return new ESMPDateTimeInterval()
                .withStart(interval.start())
                .withEnd(interval.end());
    }
}
