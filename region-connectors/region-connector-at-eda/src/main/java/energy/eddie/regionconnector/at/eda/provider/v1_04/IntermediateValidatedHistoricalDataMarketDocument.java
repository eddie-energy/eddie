// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_04;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

class IntermediateValidatedHistoricalDataMarketDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateValidatedHistoricalDataMarketDocument.class);
    private final CommonInformationModelConfiguration cimConfig;
    private final EdaConsumptionRecord consumptionRecord;
    private final List<AtPermissionRequest> permissionRequests;

    public IntermediateValidatedHistoricalDataMarketDocument(
            CommonInformationModelConfiguration cimConfig,
            IdentifiableConsumptionRecord consumptionRecord
    ) {
        this.cimConfig = cimConfig;
        this.consumptionRecord = consumptionRecord.consumptionRecord();
        this.permissionRequests = consumptionRecord.permissionRequests();
    }


    public List<VHDEnvelope> toVhd() {
        var periodInterval = new EsmpTimeInterval(
                consumptionRecord.startDate().atStartOfDay(AT_ZONE_ID),
                consumptionRecord.endDate().atStartOfDay(AT_ZONE_ID)
        );
        var vhd = new VHDMarketDocument()
                .withMRID(consumptionRecord.messageId())
                .withCreatedDateTime(consumptionRecord.documentCreationDateTime())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme().value())
                                .withValue(consumptionRecord.receiverMessageAddress())
                )
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                .withValue(consumptionRecord.senderMessageAddress())
                )
                .withPeriodTimeInterval(new ESMPDateTimeInterval()
                                                .withStart(periodInterval.start())
                                                .withEnd(periodInterval.end()))
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withTimeSeries(timeSeriesList());
        var docs = new ArrayList<VHDEnvelope>(permissionRequests.size());
        for (var permissionRequest : permissionRequests) {
            docs.add(new VhdEnvelopeWrapper(vhd, permissionRequest).wrap());
        }
        return docs;
    }

    private List<TimeSeries> timeSeriesList() {
        var timeSeriesList = new ArrayList<TimeSeries>();
        for (var energy : consumptionRecord.energy()) {
            for (var energyData : energy.energyData()) {
                timeSeriesList.add(timeSeries(energy, energyData));
            }
        }
        return timeSeriesList;
    }

    private TimeSeries timeSeries(Energy energy, EnergyData energyData) {
        return new TimeSeries()
                // This defines the evolution of the timeseries, since this is going to be the first document, it's always "1"
                .withVersion("1")
                .withDateAndOrTimeDateTime(consumptionRecord.processDate().toGregorianCalendar().toZonedDateTime())
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDString()
                                .withValue(consumptionRecord.meteringPoint())
                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                )
                .withEnergyMeasurementUnitName(measurementUnit(energyData))
                .withEnergyQualityMeasurementUnitName(StandardUnitOfMeasureTypeList.ONE.value())
                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                .withReasonText(energy.meteringReason())
                .withRegisteredResourceMRID(
                        new ResourceIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME.value())
                                .withValue(energyData.meterCode())
                )
                .withFlowDirectionDirection(flowDirection(energyData))
                .withPeriods(seriesPeriod(energy, energyData));
    }

    private SeriesPeriod seriesPeriod(Energy energy, EnergyData energyData) {
        var granularity = energy.granularity();
        var duration = granularity == null ? Duration.ZERO : granularity.duration();
        var resolution = DatatypeFactory.newDefaultInstance().newDuration(duration.toMillis());
        var interval = new EsmpTimeInterval(
                energy.meterReadingStart(),
                energy.meterReadingEnd()
        );

        return new SeriesPeriod()
                .withResolution(resolution)
                .withTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                )
                .withPoints(points(energyData));
    }

    @Nullable
    private String flowDirection(EnergyData energyData) {
        // The available meter/OBIS codes can be found on https://www.ebutilities.at/documents/20200304112759_MeterCodes_ConsumptionRecord.pdf
        // a code looks like this 1-1:1.9.0 P.01
        // Looking at the document, we can use the 5th character to determine if the meter is a consumption or production meter
        String flowDirection = null;
        var obisMeterCode = energyData.meterCode().charAt(4);
        if (obisMeterCode == '1') { // Consumption
            flowDirection = StandardDirectionTypeList.DOWN.value();
        } else if (obisMeterCode == '2') { // Production
            flowDirection = StandardDirectionTypeList.UP.value();
        } else {
            LOGGER.atInfo()
                  .addArgument(obisMeterCode)
                  .addArgument(consumptionRecord::messageId)
                  .log("Unknown meter code {} for message {}");
        }
        return flowDirection;
    }

    @Nullable
    private String measurementUnit(EnergyData energyData) {
        var funcs = List.<UnaryOperator<String>>of(
                (String r) -> StandardUnitOfMeasureTypeList.fromValue(r).value(),
                (String r) -> LocalUnitOfMeasureType.fromValue(r).value()
        );
        for (var func : funcs) {
            try {
                return func.apply(energyData.billingUnit());
            } catch (IllegalArgumentException e) {
                LOGGER.atDebug()
                      .addArgument(energyData::billingUnit)
                      .addArgument(consumptionRecord::messageId)
                      .log("Got unknown unit {} for message {}", e);
            }
        }
        return null;
    }

    private List<Point> points(EnergyData energyData) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < energyData.energyPositions().size(); i++) {
            var meterReading = energyData.energyPositions().get(i);
            points.add(new Point()
                               .withPosition(i + 1) // Position has to be a positive integer != 0
                               .withEnergyQuantityQuantity(meterReading.billingQuantity())
                               .withEnergyQuantityQuality(energyQuantityQuality(meterReading)));
        }
        return points;
    }

    @Nullable
    private String energyQuantityQuality(EnergyPosition meterReading) {
        var meteringMethod = meterReading.meteringMethod();
        return switch (meteringMethod) {
            case "L1" -> StandardQualityTypeList.AS_PROVIDED.value();
            case "L2" -> StandardQualityTypeList.ADJUSTED.value();
            case "L3" -> StandardQualityTypeList.ESTIMATED.value();
            default -> {
                LOGGER.info("Unknown metering method {} for message {}", meteringMethod, consumptionRecord.messageId());
                yield null;
            }
        };
    }
}
