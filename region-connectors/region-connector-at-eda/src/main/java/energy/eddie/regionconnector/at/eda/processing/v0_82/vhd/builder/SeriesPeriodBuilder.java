package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.Energy;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.EnergyData;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.EnergyPosition;
import energy.eddie.cim.v0_82.vhd.ESMPDateTimeIntervalComplexType;
import energy.eddie.cim.v0_82.vhd.PointComplexType;
import energy.eddie.cim.v0_82.vhd.QualityTypeList;
import energy.eddie.cim.v0_82.vhd.SeriesPeriodComplexType;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import energy.eddie.regionconnector.at.eda.utils.MeteringIntervalUtil;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;

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

        var interval = new EsmpTimeInterval(
                XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodStart()),
                XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodEnd())
        );

        seriesPeriod.withTimeInterval(new ESMPDateTimeIntervalComplexType()
                .withStart(interval.start())
                .withEnd(interval.end())
        );

        return this;
    }
}