package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.Energy;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.EnergyData;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.EnergyPosition;
import energy.eddie.cim.validated_historical_data.v0_82.ESMPDateTimeIntervalComplexType;
import energy.eddie.cim.validated_historical_data.v0_82.PointComplexType;
import energy.eddie.cim.validated_historical_data.v0_82.QualityTypeList;
import energy.eddie.cim.validated_historical_data.v0_82.SeriesPeriodComplexType;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import energy.eddie.regionconnector.at.eda.utils.MeteringIntervalUtil;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;

import java.util.ArrayList;
import java.util.List;

public class SeriesPeriodBuilder {

    private final SeriesPeriodComplexType seriesPeriod = new SeriesPeriodComplexType()
            .withReasonList(new SeriesPeriodComplexType.ReasonList());

    public SeriesPeriodBuilder withEnergyData(EnergyData energyData) throws InvalidMappingException {
        List<PointComplexType> points = new ArrayList<>();
        for (int i = 0; i < energyData.getEP().size(); i++) {
            EnergyPosition energyPosition = energyData.getEP().get(i);

            PointComplexType point = new PointComplexType();
            point.setPosition(String.valueOf(i));
            point.setEnergyQuantityQuantity(energyPosition.getBQ());
            point.setEnergyQuantityQuality(switch (energyPosition.getMM()) {
                case "L1" -> QualityTypeList.AS_PROVIDED;
                case "L2" -> QualityTypeList.ADJUSTED;
                case "L3" -> QualityTypeList.ESTIMATED;
                default ->
                        throw new InvalidMappingException("Unexpected MeteringMethod value + '" + energyPosition.getMM() + "' in consumptionRecord");
            });
            points.add(point);
        }

        seriesPeriod.withPointList(new SeriesPeriodComplexType.PointList().withPoints(points));

        return this;
    }

    public SeriesPeriodComplexType build() {
        return seriesPeriod;
    }

    public SeriesPeriodBuilder withEnergy(Energy energy) throws InvalidMappingException {
        seriesPeriod.setResolution(MeteringIntervalUtil.toGranularity(energy.getMeteringIntervall()).name());

        var intervalStart = new EsmpDateTime(XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodStart()));
        var intervalEnd = new EsmpDateTime(XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodEnd()));

        seriesPeriod.withTimeInterval(new ESMPDateTimeIntervalComplexType()
                .withStart(intervalStart.toString())
                .withEnd(intervalEnd.toString())
        );

        return this;
    }
}