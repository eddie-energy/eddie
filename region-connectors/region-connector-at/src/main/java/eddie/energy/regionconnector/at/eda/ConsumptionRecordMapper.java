package eddie.energy.regionconnector.at.eda;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.ConsumptionRecord;
import eddie.energy.regionconnector.api.v0.models.ConsumptionPoint;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

public class ConsumptionRecordMapper {

    private static ZonedDateTime xmlGregorianCalendarToZonedDateTime(XMLGregorianCalendar xmlGregorianCalendar) {
        // Convert XMLGregorianCalendar to GregorianCalendar
        GregorianCalendar gregorianCalendar = xmlGregorianCalendar.toGregorianCalendar();

        // Convert GregorianCalendar to Instant
        java.time.Instant instant = gregorianCalendar.toZonedDateTime().toInstant();

        // Get ZoneId from XMLGregorianCalendar
        ZoneId zoneId = gregorianCalendar.getTimeZone().toZoneId();

        // Create ZonedDateTime using Instant and ZoneId

        return ZonedDateTime.ofInstant(instant, zoneId);
    }

    public static eddie.energy.regionconnector.api.v0.models.ConsumptionRecord mapToCIM(ConsumptionRecord externalConsumptionRecord) {
        var consumptionRecord = new eddie.energy.regionconnector.api.v0.models.ConsumptionRecord();
        var energy = externalConsumptionRecord.getProcessDirectory().getEnergy().stream().findFirst().orElseThrow();
        consumptionRecord.setMeteringPoint(externalConsumptionRecord.getProcessDirectory().getMeteringPoint());
        consumptionRecord.setStartDateTime(xmlGregorianCalendarToZonedDateTime(energy.getMeteringPeriodStart()));
        consumptionRecord.setMeteringInterval(switch (energy.getMeteringIntervall()) {
            case D -> eddie.energy.regionconnector.api.v0.models.ConsumptionRecord.MeteringInterval.PT_1_D;
            case H -> eddie.energy.regionconnector.api.v0.models.ConsumptionRecord.MeteringInterval.PT_1_H;
            case QH -> eddie.energy.regionconnector.api.v0.models.ConsumptionRecord.MeteringInterval.PT_15_M;
            default -> throw new IllegalStateException("Unexpected value: " + energy.getMeteringIntervall());
        });
        var energyData = energy.getEnergyData().stream().findFirst().orElseThrow();
        var conversionFactor = switch (energyData.getUOM()) {
            case KWH -> 1000;
            case GWH -> 1000000;
            case MWH -> 1000000000;
            default -> throw new IllegalStateException("Unexpected value: " + energyData.getUOM());
        };
        energyData.getEP().forEach(energyPosition -> {
            var dataPoint = new eddie.energy.regionconnector.api.v0.models.ConsumptionPoint();
            dataPoint.setMeteringType(switch (energyPosition.getMM()) {
                case "L1" -> ConsumptionPoint.MeteringType.MEASURED_VALUE;
                default -> ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE;
            });
            dataPoint.setConsumption(energyPosition.getBQ().doubleValue() * conversionFactor);
            consumptionRecord.getConsumptionPoints().add(dataPoint);
        });

        return consumptionRecord;
    }
}
