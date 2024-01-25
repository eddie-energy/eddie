package energy.eddie.regionconnector.at.eda.processing.mvp1;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.utils.ConversionFactor;
import energy.eddie.regionconnector.at.eda.utils.DateTimeConstants;
import energy.eddie.regionconnector.at.eda.utils.MeteringIntervalUtil;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Component
public class Mvp1ConsumptionRecordMapper {

    public static final String MEASURED_VALUE = "L1";

    private ZonedDateTime toZonedDateTime(XMLGregorianCalendar xmlGregorianCalendar) {
        // Convert XMLGregorianCalendar to GregorianCalendar
        GregorianCalendar gregorianCalendar = xmlGregorianCalendar.toGregorianCalendar();
        return gregorianCalendar.toZonedDateTime().withZoneSameLocal(DateTimeConstants.AT_ZONE_ID);
    }

    /**
     * Maps an EDA consumption record to a MVP1 consumption record
     *
     * @param externalConsumptionRecord The external consumption record to map
     * @return a CIM consumption record
     * @throws InvalidMappingException if the mapping cant be completed because of invalid {@link ConsumptionRecord}. This can happen if the {@link ConsumptionRecord} is missing required fields such as Energy and EnergyData
     */
    public energy.eddie.api.v0.ConsumptionRecord mapToMvp1ConsumptionRecord(ConsumptionRecord externalConsumptionRecord) throws InvalidMappingException {
        requireNonNull(externalConsumptionRecord);

        var consumptionRecord = new energy.eddie.api.v0.ConsumptionRecord();

        var crEnergy = externalConsumptionRecord.getProcessDirectory().getEnergy().stream().findFirst().orElseThrow(() -> new InvalidMappingException("No Energy found in ProcessDirectory of ConsumptionRecord"));
        consumptionRecord.setMeteringPoint(externalConsumptionRecord.getProcessDirectory().getMeteringPoint());
        consumptionRecord.setStartDateTime(toZonedDateTime(crEnergy.getMeteringPeriodStart()));
        consumptionRecord.setMeteringInterval(MeteringIntervalUtil.toGranularity(crEnergy.getMeteringIntervall()).name());

        var energyData = crEnergy.getEnergyData().stream().findFirst().orElseThrow(() -> new InvalidMappingException("No EnergyData found in Energy of ConsumptionRecord"));
        var conversionFactor = switch (energyData.getUOM()) {
            case KWH -> ConversionFactor.KWH_TO_WH;
            case MWH -> ConversionFactor.MWH_TO_WH;
            case GWH -> ConversionFactor.GWH_TO_WH;
            default -> throw new IllegalStateException("Unexpected value: " + energyData.getUOM());
        };

        List<ConsumptionPoint> consumptionPoints = new ArrayList<>(crEnergy.getNumberOfMeteringIntervall().intValue());
        energyData.getEP().forEach(energyPosition -> {
            var dataPoint = new energy.eddie.api.v0.ConsumptionPoint();

            if (Objects.equals(energyPosition.getMM(), MEASURED_VALUE)) {
                dataPoint.setMeteringType(ConsumptionPoint.MeteringType.MEASURED_VALUE);
            } else {
                dataPoint.setMeteringType(ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE);
            }

            dataPoint.setConsumption(energyPosition.getBQ().doubleValue() * conversionFactor.getFactor());
            consumptionPoints.add(dataPoint);
        });
        consumptionRecord.withConsumptionPoints(consumptionPoints);

        return consumptionRecord;
    }
}