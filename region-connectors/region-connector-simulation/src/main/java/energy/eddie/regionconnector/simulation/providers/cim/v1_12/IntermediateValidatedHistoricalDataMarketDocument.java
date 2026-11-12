// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_12;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v1_12.*;
import energy.eddie.cim.v1_12.vhd.*;
import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_12.VhdEnvelopeWrapper;
import energy.eddie.regionconnector.simulation.SimulationConnectorMetadata;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.permission.request.SimulationPermissionRequest;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class IntermediateValidatedHistoricalDataMarketDocument {
    private final HistoricalDataEmissionMetadata metadata;
    private final SimulatedMeterReading simulatedMeterReading;
    private final CommonInformationModelConfiguration cimConfig;

    public IntermediateValidatedHistoricalDataMarketDocument(
            SimulatedMeterReading simulatedMeterReading,
            CommonInformationModelConfiguration cimConfig
    ) {
        this(simulatedMeterReading, cimConfig, new HistoricalDataEmissionMetadata());
    }

    public IntermediateValidatedHistoricalDataMarketDocument(
            SimulatedMeterReading simulatedMeterReading,
            CommonInformationModelConfiguration cimConfig,
            HistoricalDataEmissionMetadata metadata
    ) {
        this.metadata = metadata;
        this.simulatedMeterReading = simulatedMeterReading;
        this.cimConfig = cimConfig;
    }

    public Envelope value() {
        ZonedDateTime endDate = simulatedMeterReading
                .startDateTime()
                .plus(Duration.parse(simulatedMeterReading.meteringInterval())
                              .multipliedBy(simulatedMeterReading.measurements().size())
                );
        var timeframe = new EsmpTimeInterval(
                simulatedMeterReading.startDateTime(),
                endDate
        );

        var vhd = new VHDMarketDocument()
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.EIC.value())
                                .withValue(SimulationConnectorMetadata.REGION_CONNECTOR_ID)
                )
                .withMRID(metadata.id("document"))
                .withCreatedDateTime(metadata.created(ZoneOffset.UTC))
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme().value())
                                .withValue(SimulationConnectorMetadata.REGION_CONNECTOR_ID)
                )
                .withTimeSeries(
                        new TimeSeries()
                                .withMRID(metadata.id("series"))
                                .withBusinessType(StandardBusinessTypeList.CONSUMPTION.value())
                                .withProduct(StandardEnergyProductTypeList.ACTIVE_ENERGY.value())
                                .withVersion("1")
                                .withFlowDirectionDirection(StandardDirectionTypeList.DOWN.value())
                                .withAccountingPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind._26)
                                .withAccountingPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind._0)
                                .withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.WATT.value())
                                .withAccountingPointMRID(
                                        new MeasurementPointIDString()
                                                .withCodingScheme(StandardCodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME.value())
                                                .withValue(simulatedMeterReading.meteringPoint())
                                )
                                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                                .withSeriesPeriods(
                                        seriesPeriods(simulatedMeterReading, timeframe)
                                )
                );

        return new VhdEnvelopeWrapper(
                vhd,
                new SimulationPermissionRequest(
                        simulatedMeterReading.connectionId(),
                        simulatedMeterReading.permissionId(),
                        simulatedMeterReading.dataNeedId(),
                        PermissionProcessStatus.ACCEPTED
                ),
                metadata.envelopeCreated()
        ).wrap();
    }

    private List<SeriesPeriod> seriesPeriods(
            SimulatedMeterReading simulatedMeterReading,
            EsmpTimeInterval interval
    ) {
        String resolution = simulatedMeterReading.meteringInterval();
        List<Point> points = new ArrayList<>();
        int position = 1;
        for (var measurement : simulatedMeterReading.measurements()) {
            var point = new Point()
                    .withPosition(position)
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(measurement.value()))
                    .withEnergyQuantityQuality(
                            measurement.measurementType() == Measurement.MeasurementType.MEASURED
                                    ? StandardQualityTypeList.AS_PROVIDED.value()
                                    : StandardQualityTypeList.ADJUSTED.value()
                    );
            points.add(point);
            position++;
        }

        var seriesPeriod = new SeriesPeriod()
                .withResolution(DatatypeFactory.newDefaultInstance().newDuration(Duration.parse(resolution).toMillis()))
                .withTimeInterval(new ESMPDateTimeInterval()
                                          .withStart(interval.start())
                                          .withEnd(interval.end())
                )
                .withPoints(points)
                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value());
        return List.of(seriesPeriod);
    }
}
