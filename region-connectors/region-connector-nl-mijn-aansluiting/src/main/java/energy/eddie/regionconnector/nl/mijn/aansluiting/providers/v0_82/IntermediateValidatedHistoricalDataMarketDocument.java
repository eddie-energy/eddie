// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.providers.MeteredDataRegisters;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.*;

class IntermediateValidatedHistoricalDataMarketDocument {

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

    @SuppressWarnings("java:S3776") // The mapping code for the CIM is this long
    public List<ValidatedHistoricalDataEnvelope> toEddieValidatedHistoricalDataMarketDocuments() {
        var vhds = new ArrayList<ValidatedHistoricalDataEnvelope>();
        for (MijnAansluitingResponse mijnAansluitingResponse : mijnAansluitingResponses) {
            var marketEvaluationPoint = mijnAansluitingResponse.marketEvaluationPoint();
            var vhdInterval = getIntervalFromRegisters(marketEvaluationPoint.registerList());
            var vhd = new ValidatedHistoricalDataMarketDocumentComplexType()
                    .withMRID(UUID.randomUUID().toString())
                    .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                    .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
                    .withCreatedDateTime(EsmpDateTime.now().toString())
                    .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                    .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                    .withProcessProcessType(ProcessTypeList.REALISED)
                    .withReceiverMarketParticipantMRID(
                            new PartyIDStringComplexType()
                                    .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme())
                                    .withValue(mijnAansluitingConfig.continuousClientId().getValue())
                    )
                    .withSenderMarketParticipantMRID(new PartyIDStringComplexType()
                                                             .withCodingScheme(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME)
                                                             .withValue("EDSN")
                    )
                    .withPeriodTimeInterval(
                            new ESMPDateTimeIntervalComplexType()
                                    .withStart(vhdInterval.start())
                                    .withEnd(vhdInterval.end())
                    );
            var timeSeriesList = new ArrayList<TimeSeriesComplexType>();
            var registers = MeteredDataRegisters.sumUpRegistersWithSameObisCode(marketEvaluationPoint.registerList());
            for (var register : registers) {
                var flowDirection = obisToFlowDirection(register.mrid());
                if (register.readingList().isEmpty() || flowDirection == null) {
                    continue;
                }
                var interval = new EsmpTimeInterval(register.readingList()
                                                            .getFirst()
                                                            .dateAndOrTime()
                                                            .dateTime(),
                                                    register.readingList()
                                                            .getLast()
                                                            .dateAndOrTime()
                                                            .dateTime());

                var readingType = register.readingList().getFirst().readingType();
                var isEnergy = register.meter()
                                       .mrid()
                                       .startsWith("E");
                var readingUnit = MeteredDataRegisters.readingUnit(readingType);

                var pointList = new ArrayList<PointComplexType>();
                for (var reading : register.readingList()) {
                    pointList.add(
                            new PointComplexType()
                                    .withPosition(reading.dateAndOrTime().dateTime().toString())
                                    .withEnergyQuantityQuality(QualityTypeList.AS_PROVIDED)
                                    .withEnergyQuantityQuantity(reading.value())
                    );
                }
                timeSeriesList.add(
                        new TimeSeriesComplexType()
                                .withRegisteredResource(
                                        new RegisteredResourceComplexType()
                                                .withMRID(register.meter().mrid())
                                )
                                .withBusinessType(flowDirection == DirectionTypeList.DOWN ? BusinessTypeList.CONSUMPTION : BusinessTypeList.PRODUCTION)
                                .withProduct(EnergyProductTypeList.ACTIVE_POWER)
                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(isEnergy ? CommodityKind.ELECTRICITYPRIMARYMETERED : CommodityKind.NATURALGAS)
                                .withMarketEvaluationPointMRID(
                                        new MeasurementPointIDStringComplexType()
                                                .withCodingScheme(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME)
                                                .withValue(marketEvaluationPoint.mrid())
                                )
                                .withFlowDirectionDirection(flowDirection)
                                .withEnergyMeasurementUnitName(
                                        readingUnit.equals("M3")
                                                ? UnitOfMeasureTypeList.CUBIC_METRE
                                                : UnitOfMeasureTypeList.fromValue(readingUnit)
                                )
                                .withSeriesPeriodList(
                                        new TimeSeriesComplexType.SeriesPeriodList()
                                                .withSeriesPeriods(
                                                        new SeriesPeriodComplexType()
                                                                .withTimeInterval(
                                                                        new ESMPDateTimeIntervalComplexType()
                                                                                .withStart(interval.start())
                                                                                .withEnd(interval.end())
                                                                )
                                                                .withPointList(
                                                                        new SeriesPeriodComplexType.PointList()
                                                                                .withPoints(pointList)
                                                                )
                                                                .withResolution("P1D")
                                                )
                                )
                );
            }
            if (timeSeriesList.isEmpty()) {
                continue;
            }
            vhd.withTimeSeriesList(new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                           .withTimeSeries(timeSeriesList));
            vhds.add(
                    new VhdEnvelope(vhd, permissionRequest).wrap()

            );
        }
        return vhds;
    }

    private EsmpTimeInterval getIntervalFromRegisters(List<Register> registerList) {
        ZonedDateTime start = null;
        ZonedDateTime end = null;
        for (var register : registerList) {
            var readingStart = register.readingList().getFirst().dateAndOrTime().dateTime();
            var readingEnd = register.readingList().getLast().dateAndOrTime().dateTime();
            if (start == null || start.isAfter(readingStart)) {
                start = readingStart;
            }
            if (end == null || end.isBefore(readingEnd)) {
                end = readingEnd;
            }
        }
        return new EsmpTimeInterval(start, end);
    }

    @Nullable
    private DirectionTypeList obisToFlowDirection(String obisCode) {
        // See this documentation for OBIS codes: https://www.promotic.eu/en/pmdoc/Subsystems/Comm/PmDrivers/PmIEC62056/IEC62056_OBIS.htm
        if (obisCode.startsWith("1.8")) {
            return DirectionTypeList.DOWN;
        }
        if (obisCode.startsWith("2.8")) {
            return DirectionTypeList.UP;
        }
        return null;
    }
}
