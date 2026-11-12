// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v1_12;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_12.*;
import energy.eddie.cim.v1_12.vhd.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Register;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.providers.MeteredDataRegisters;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_12.vhd.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Maps the metered data of Mijn Aansluiting to CIM v1.12 validated historical data market documents.
 * Contains the same information as the CIM v0.82 mapping.
 * In contrast to CIM v0.82, which uses the timestamp of a reading as its position,
 * CIM v1.12 requires integer positions, so the readings are numbered consecutively starting at 1.
 */
class IntermediateValidatedHistoricalDataMarketDocument {
    private static final String DAILY_RESOLUTION = "P1D";
    private final CommonInformationModelConfiguration cimConfig;
    private final MijnAansluitingConfiguration mijnAansluitingConfig;
    private final MijnAansluitingPermissionRequest permissionRequest;
    private final List<MijnAansluitingResponse> mijnAansluitingResponses;

    IntermediateValidatedHistoricalDataMarketDocument(
            CommonInformationModelConfiguration cimConfig,
            MijnAansluitingConfiguration mijnAansluitingConfig,
            IdentifiableMeteredData identifiableMeteredData
    ) {
        this.cimConfig = cimConfig;
        this.mijnAansluitingConfig = mijnAansluitingConfig;
        this.permissionRequest = identifiableMeteredData.permissionRequest();
        this.mijnAansluitingResponses = identifiableMeteredData.meteredData();
    }

    public List<Envelope> toVhds() {
        var vhds = new ArrayList<Envelope>();
        for (MijnAansluitingResponse mijnAansluitingResponse : mijnAansluitingResponses) {
            var marketEvaluationPoint = mijnAansluitingResponse.marketEvaluationPoint();
            var created = ZonedDateTime.now(ZoneOffset.UTC);
            var timeSeriesList = new ArrayList<TimeSeries>();
            var registers = MeteredDataRegisters.sumUpRegistersWithSameObisCode(marketEvaluationPoint.registerList());
            for (var register : registers) {
                var flowDirection = obisToFlowDirection(register.mrid());
                if (register.readingList().isEmpty() || flowDirection == null) {
                    continue;
                }
                timeSeriesList.add(timeSeries(register, flowDirection, marketEvaluationPoint.mrid(), created));
            }
            if (timeSeriesList.isEmpty()) {
                continue;
            }
            var vhd = new VHDMarketDocument()
                    .withMRID(UUID.randomUUID().toString())
                    .withRevisionNumber(CommonInformationModelVersions.V1_12.cimify())
                    .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                    .withCreatedDateTime(created)
                    .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                    .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                    .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                    .withReceiverMarketParticipantMRID(
                            new PartyIDString()
                                    .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme().value())
                                    .withValue(mijnAansluitingConfig.continuousClientId().getValue())
                    )
                    .withSenderMarketParticipantMRID(
                            new PartyIDString()
                                    .withCodingScheme(StandardCodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME.value())
                                    .withValue("EDSN")
                    )
                    .withTimeSeries(timeSeriesList);
            vhds.add(new VhdEnvelopeWrapper(vhd, permissionRequest).wrap());
        }
        return vhds;
    }

    private static TimeSeries timeSeries(
            Register register,
            StandardDirectionTypeList flowDirection,
            String meteringPointId,
            ZonedDateTime created
    ) {
        var readings = register.readingList();
        var interval = new EsmpTimeInterval(readings.getFirst().dateAndOrTime().dateTime(),
                                            readings.getLast().dateAndOrTime().dateTime());
        var isEnergy = register.meter().mrid().startsWith("E");
        var readingUnit = MeteredDataRegisters.readingUnit(readings.getFirst().readingType());

        var points = new ArrayList<Point>(readings.size());
        for (int i = 0; i < readings.size(); i++) {
            points.add(
                    new Point()
                            .withPosition(i + 1)
                            .withEnergyQuantityQuality(StandardQualityTypeList.AS_PROVIDED.value())
                            .withEnergyQuantityQuantity(readings.get(i).value())
            );
        }
        return new TimeSeries()
                .withVersion("1")
                // dateAndOrTime.dateTime is mandatory in CIM v1.12, use the creation time of the document
                .withDateAndOrTimeDateTime(created)
                // The coding scheme is mandatory in CIM v1.12, the meter ID is a Dutch national identifier
                .withRegisteredResourceMRID(
                        new ResourceIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME.value())
                                .withValue(register.meter().mrid())
                )
                .withBusinessType(flowDirection == StandardDirectionTypeList.DOWN
                                          ? StandardBusinessTypeList.CONSUMPTION.value()
                                          : StandardBusinessTypeList.PRODUCTION.value())
                .withProduct(StandardEnergyProductTypeList.ACTIVE_POWER.value())
                // 2 = electricity primary metered, 7 = natural gas
                .withAccountingPointMeterReadingsReadingsReadingTypeCommodity(isEnergy ? CommodityKind._2 : CommodityKind._7)
                .withAccountingPointMRID(
                        new MeasurementPointIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME.value())
                                .withValue(meteringPointId)
                )
                .withFlowDirectionDirection(flowDirection.value())
                .withEnergyMeasurementUnitName(
                        readingUnit.equals("M3")
                                ? StandardUnitOfMeasureTypeList.CUBIC_METRE.value()
                                : StandardUnitOfMeasureTypeList.fromValue(readingUnit).value()
                )
                .withSeriesPeriods(
                        new SeriesPeriod()
                                .withResolution(DatatypeFactory.newDefaultInstance().newDuration(DAILY_RESOLUTION))
                                .withTimeInterval(
                                        new ESMPDateTimeInterval()
                                                .withStart(interval.start())
                                                .withEnd(interval.end())
                                )
                                .withPoints(points)
                );
    }

    @Nullable
    private static StandardDirectionTypeList obisToFlowDirection(String obisCode) {
        // See this documentation for OBIS codes: https://www.promotic.eu/en/pmdoc/Subsystems/Comm/PmDrivers/PmIEC62056/IEC62056_OBIS.htm
        if (obisCode.startsWith("1.8")) {
            return StandardDirectionTypeList.DOWN;
        }
        if (obisCode.startsWith("2.8")) {
            return StandardDirectionTypeList.UP;
        }
        return null;
    }
}
