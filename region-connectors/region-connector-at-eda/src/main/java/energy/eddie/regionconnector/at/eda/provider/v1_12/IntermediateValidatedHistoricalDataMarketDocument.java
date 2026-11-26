// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_12.*;
import energy.eddie.cim.v1_12.vhd.*;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.at.eda.provider.EdaMeteringSeries;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_12.vhd.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

class IntermediateValidatedHistoricalDataMarketDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateValidatedHistoricalDataMarketDocument.class);
    private final CommonInformationModelConfiguration cimConfig;
    private final EdaConsumptionRecord consumptionRecord;
    private final List<AtPermissionRequest> permissionRequests;

    IntermediateValidatedHistoricalDataMarketDocument(
            CommonInformationModelConfiguration cimConfig,
            IdentifiableConsumptionRecord consumptionRecord
    ) {
        this.cimConfig = cimConfig;
        this.consumptionRecord = consumptionRecord.consumptionRecord();
        this.permissionRequests = consumptionRecord.permissionRequests();
    }

    public List<Envelope> toVhd() {
        var docs = new ArrayList<Envelope>(permissionRequests.size());
        for (var permissionRequest : permissionRequests) {
            // Each envelope gets its own market document, so no state is shared between envelopes
            docs.add(new VhdEnvelopeWrapper(marketDocument(), permissionRequest).wrap());
        }
        return docs;
    }

    private VHDMarketDocument marketDocument() {
        return new VHDMarketDocument()
                .withMRID(consumptionRecord.messageId())
                .withRevisionNumber(CommonInformationModelVersions.V1_12.cimify())
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
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withTimeSeries(timeSeriesList());
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
                .withDateAndOrTimeDateTime(consumptionRecord.processDate())
                .withAccountingPointMRID(
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
                .withSeriesPeriods(seriesPeriod(energy, energyData));
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
        var obisMeterCode = energyData.meterCode().charAt(4);
        return switch (obisMeterCode) {
            case '1' -> StandardDirectionTypeList.DOWN.value(); // Consumption
            case '2' -> StandardDirectionTypeList.UP.value(); // Production
            default -> {
                LOGGER.atInfo()
                      .addArgument(obisMeterCode)
                      .addArgument(consumptionRecord::messageId)
                      .log("Unknown meter code {} for message {}");
                yield null;
            }
        };
    }

    @Nullable
    private String measurementUnit(EnergyData energyData) {
        return EdaMeteringSeries.measurementUnit(
                energyData,
                consumptionRecord.messageId(),
                unit -> isKnown(unit, code -> StandardUnitOfMeasureTypeList.fromValue(code).value())
                        || isKnown(unit, code -> LocalUnitOfMeasureType.fromValue(code).value())
        );
    }

    private List<Point> points(EnergyData energyData) {
        List<Point> points = new ArrayList<>();
        for (var point : EdaMeteringSeries.points(energyData, consumptionRecord.messageId())) {
            points.add(new Point()
                               .withPosition(point.position())
                               .withEnergyQuantityQuantity(point.quantity())
                               .withEnergyQuantityQuality(point.quality()));
        }
        return points;
    }

    private static boolean isKnown(String code, UnaryOperator<String> converter) {
        try {
            return converter.apply(code) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
