package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Reading;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.ReadingType;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Register;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class IntermediateValidatedHistoricalDataMarketDocument {

    private final CommonInformationModelConfiguration cimConfig;
    private final MijnAansluitingConfiguration mijnAansluitingConfig;
    private final NlPermissionRequest permissionRequest;
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

    // TODO: get real CIM mapping, see GH-928
    @SuppressWarnings("java:S3776") // The mapping code for the CIM is this long
    public List<ValidatedHistoricalDataEnvelope> toEddieValidatedHistoricalDataMarketDocuments() {
        var vhds = new ArrayList<ValidatedHistoricalDataEnvelope>();
        for (MijnAansluitingResponse mijnAansluitingResponse : mijnAansluitingResponses) {
            var marketEvaluationPoint = mijnAansluitingResponse.getMarketEvaluationPoint();
            var vhd = new ValidatedHistoricalDataMarketDocumentComplexType()
                    .withMRID(marketEvaluationPoint.getMRID())
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
                                                             .withValue("Stichting Mijn Aansluiting")
                    );
            var timeSeriesList = new ArrayList<TimeSeriesComplexType>();
            for (Register register : marketEvaluationPoint.getRegisterList()) {
                var flowDirection = obisToFlowDirection(register.getMRID());
                if (register.getReadingList().isEmpty() || flowDirection == null) {
                    continue;
                }
                var interval = new EsmpTimeInterval(register.getReadingList()
                                                            .getFirst()
                                                            .getDateAndOrTime()
                                                            .getDateTime(),
                                                    register.getReadingList()
                                                            .getLast()
                                                            .getDateAndOrTime()
                                                            .getDateTime());

                ReadingType readingType = register.getReadingList().getFirst().getReadingType();
                boolean isEnergy = register.getMeter()
                                           .getMRID()
                                           .startsWith("E");
                var readingUnit = readingUnit(readingType);

                var pointList = new ArrayList<PointComplexType>();
                int i = 0;
                for (Reading reading : register.getReadingList()) {
                    pointList.add(
                            new PointComplexType()
                                    .withPosition(String.valueOf(i))
                                    .withEnergyQuantityQuality(QualityTypeList.ADJUSTED)
                                    .withEnergyQuantityQuantity(reading.getValue())
                    );
                    i++;
                }
                timeSeriesList.add(
                        new TimeSeriesComplexType()
                                .withMRID(register.getMRID())
                                .withBusinessType(isEnergy ? BusinessTypeList.AGGREGATED_ENERGY_DATA : BusinessTypeList.CONSUMPTION)
                                .withProduct(isEnergy ? EnergyProductTypeList.ACTIVE_ENERGY : null) // TODO: No mapping for gas, see GH-928
                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAccumulate(AccumulationKind.DELTADATA)
                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(isEnergy ? CommodityKind.ELECTRICITYPRIMARYMETERED : CommodityKind.NATURALGAS)
                                .withMarketEvaluationPointMRID(
                                        new MeasurementPointIDStringComplexType()
                                                .withCodingScheme(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME)
                                                .withValue(register.getMeter().getMRID())
                                )
                                .withReasonList(
                                        new TimeSeriesComplexType.ReasonList()
                                                .withReasons(
                                                        new ReasonComplexType()
                                                                .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
                                                )
                                )
                                .withFlowDirectionDirection(flowDirection)
                                .withEnergyMeasurementUnitName(
                                        readingUnit.equals("M3")
                                                ? null // TODO: there is no unit for cubic metres, see GH-928
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

    @Nullable
    private DirectionTypeList obisToFlowDirection(String obisCode) {
        // See this documentation for OBIS codes: https://www.promotic.eu/en/pmdoc/Subsystems/Comm/PmDrivers/IEC62056_OBIS.htm
        if (obisCode.startsWith("1.8")) {
            return DirectionTypeList.UP;
        }
        if (obisCode.startsWith("2.8")) {
            return DirectionTypeList.DOWN;
        }
        return null;
    }

    private static String readingUnit(ReadingType readingType) {
        var multiplier = (readingType.getMultiplier() == null ? "" : readingType.getMultiplier().toString());
        var readingUnit = multiplier + readingType.getUnit();
        return readingUnit.toUpperCase(Locale.ROOT);
    }
}
