package energy.eddie.regionconnector.at.eda.processing.mvp1;

import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.utils.ConversionFactor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static java.util.Objects.requireNonNull;

@Component
public class Mvp1ConsumptionRecordMapper {

    public static final String MEASURED_VALUE = "L1";

    /**
     * Maps an EDA consumption record to a MVP1 consumption record
     *
     * @param externalConsumptionRecord The external consumption record to map
     * @return a CIM consumption record
     * @throws InvalidMappingException if the mapping cant be completed because of billing unit
     */
    public energy.eddie.api.v0.ConsumptionRecord mapToMvp1ConsumptionRecord(EdaConsumptionRecord externalConsumptionRecord) throws InvalidMappingException {
        requireNonNull(externalConsumptionRecord);

        var consumptionRecord = new energy.eddie.api.v0.ConsumptionRecord();

        var crEnergy = externalConsumptionRecord.energy().getFirst();
        consumptionRecord.setMeteringPoint(externalConsumptionRecord.meteringPoint());
        consumptionRecord.setStartDateTime(externalConsumptionRecord.startDate().atStartOfDay(AT_ZONE_ID));
        consumptionRecord.setMeteringInterval(crEnergy.granularity().name());

        var energyData = crEnergy.energyData().getFirst();
        var conversionFactor = switch (energyData.billingUnit()) {
            case "KWH" -> ConversionFactor.KWH_TO_WH;
            case "MWH" -> ConversionFactor.MWH_TO_WH;
            case "GWH" -> ConversionFactor.GWH_TO_WH;
            default ->
                    throw new InvalidMappingException("Unexpected value for billing unit: " + energyData.billingUnit());
        };

        List<ConsumptionPoint> consumptionPoints = new ArrayList<>(energyData.energyPositions().size());
        energyData.energyPositions().forEach(energyPosition -> {
            var dataPoint = new energy.eddie.api.v0.ConsumptionPoint();

            if (Objects.equals(energyPosition.meteringMethod(), MEASURED_VALUE)) {
                dataPoint.setMeteringType(ConsumptionPoint.MeteringType.MEASURED_VALUE);
            } else {
                dataPoint.setMeteringType(ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE);
            }

            dataPoint.setConsumption(energyPosition.billingQuantity().doubleValue() * conversionFactor.getFactor());
            consumptionPoints.add(dataPoint);
        });
        consumptionRecord.withConsumptionPoints(consumptionPoints);

        return consumptionRecord;
    }
}
