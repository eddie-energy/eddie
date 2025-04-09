package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.*;

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

    @SuppressWarnings("java:S3776") // The mapping code for the CIM is this long
    public List<ValidatedHistoricalDataEnvelope> toEddieValidatedHistoricalDataMarketDocuments() {
        var vhds = new ArrayList<ValidatedHistoricalDataEnvelope>();
        for (MijnAansluitingResponse mijnAansluitingResponse : mijnAansluitingResponses) {
            var marketEvaluationPoint = mijnAansluitingResponse.getMarketEvaluationPoint();
            var vhdInterval = getIntervalFromRegisters(marketEvaluationPoint.getRegisterList());
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
            var registers = sumUpRegistersWithSameObisCode(marketEvaluationPoint.getRegisterList());
            for (var register : registers) {
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

                var readingType = register.getReadingList().getFirst().getReadingType();
                var isEnergy = register.getMeter()
                                       .getMRID()
                                       .startsWith("E");
                var readingUnit = readingUnit(readingType);

                var pointList = new ArrayList<PointComplexType>();
                for (var reading : register.getReadingList()) {
                    pointList.add(
                            new PointComplexType()
                                    .withPosition(reading.getDateAndOrTime().getDateTime().toString())
                                    .withEnergyQuantityQuality(QualityTypeList.AS_PROVIDED)
                                    .withEnergyQuantityQuantity(reading.getValue())
                    );
                }
                timeSeriesList.add(
                        new TimeSeriesComplexType()
                                .withRegisteredResource(
                                        new RegisteredResourceComplexType()
                                                .withMRID(register.getMeter().getMRID())
                                )
                                .withBusinessType(flowDirection == DirectionTypeList.DOWN ? BusinessTypeList.CONSUMPTION : BusinessTypeList.PRODUCTION)
                                .withProduct(EnergyProductTypeList.ACTIVE_POWER)
                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(isEnergy ? CommodityKind.ELECTRICITYPRIMARYMETERED : CommodityKind.NATURALGAS)
                                .withMarketEvaluationPointMRID(
                                        new MeasurementPointIDStringComplexType()
                                                .withCodingScheme(CodingSchemeTypeList.NETHERLANDS_NATIONAL_CODING_SCHEME)
                                                .withValue(marketEvaluationPoint.getMRID())
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

    private static List<Register> sumUpRegistersWithSameObisCode(List<Register> registers) {
        var partitions = partitionRegistersByMeter(registers);
        var partitionedByMeterAndObisCode = partitionByObisCode(partitions);
        return partitionedByMeterAndObisCode.stream()
                                            .map(partition -> partition.stream()
                                                                       .reduce(IntermediateValidatedHistoricalDataMarketDocument::mergeAndSum))
                                            .filter(Optional::isPresent)
                                            .map(Optional::get)
                                            .toList();
    }

    private static ArrayList<List<Register>> partitionByObisCode(List<List<Register>> partitions) {
        var partitionedByMeterAndObisCode = new ArrayList<List<Register>>();
        for (var partition : partitions) {
            var map = new HashMap<String, List<Register>>();
            for (var register : partition) {
                var obisCode = register.getMRID();
                var code = switch (obisCode.subSequence(0, 3).toString()) {
                    case "1.8" -> "1.8";
                    case "2.8" -> "2.8";
                    default -> obisCode;
                };
                var res = map.getOrDefault(code, new ArrayList<>());
                res.add(register);
                map.put(code, res);
            }
            partitionedByMeterAndObisCode.addAll(map.values());
        }
        return partitionedByMeterAndObisCode;
    }

    private static Register mergeAndSum(Register lr, Register rr) {
        var register = new Register()
                .meter(lr.getMeter());
        register.setMRID(lr.getMRID());
        Map<DateAndOrTime, Reading> map = new HashMap<>();

        for (var reading : lr.getReadingList()) {
            map.put(reading.getDateAndOrTime(), new Reading()
                    .dateAndOrTime(reading.getDateAndOrTime())
                    .value(reading.getValue())
                    .readingType(reading.getReadingType()));
        }

        for (var reading : rr.getReadingList()) {
            map.merge(reading.getDateAndOrTime(), reading,
                      (existing, incoming) -> existing.value(existing.getValue().add(incoming.getValue()))
            );
        }

        var readingList = new ArrayList<>(map.values());
        readingList.sort(Comparator.comparing(r -> r.getDateAndOrTime().getDateTime()));
        return register.readingList(readingList);
    }

    private static List<List<Register>> partitionRegistersByMeter(List<Register> registers) {
        var map = new HashMap<String, List<Register>>();
        for (var register : registers) {
            var mrid = register.getMeter().getMRID();
            var res = map.getOrDefault(mrid, new ArrayList<>());
            res.add(register);
            map.put(mrid, res);
        }
        return new ArrayList<>(map.values());
    }

    private EsmpTimeInterval getIntervalFromRegisters(List<Register> registerList) {
        ZonedDateTime start = null;
        ZonedDateTime end = null;
        for (var register : registerList) {
            var readingStart = register.getReadingList().getFirst().getDateAndOrTime().getDateTime();
            var readingEnd = register.getReadingList().getLast().getDateAndOrTime().getDateTime();
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

    private static String readingUnit(ReadingType readingType) {
        var multiplier = (readingType.getMultiplier() == null ? "" : readingType.getMultiplier().toString());
        var readingUnit = multiplier + readingType.getUnit();
        return readingUnit.toUpperCase(Locale.ROOT);
    }
}
