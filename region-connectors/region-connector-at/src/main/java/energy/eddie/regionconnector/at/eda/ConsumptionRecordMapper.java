package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30.ConsumptionRecord;
import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.regionconnector.at.eda.utils.ConversionFactor;
import jakarta.annotation.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class ConsumptionRecordMapper {

    private final ZoneId zoneId;

    /**
     * @param zoneId The zoneId of the time zone the dates should be converted to
     */
    public ConsumptionRecordMapper(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    private ZonedDateTime toZonedDateTime(XMLGregorianCalendar xmlGregorianCalendar) {
        // Convert XMLGregorianCalendar to GregorianCalendar
        GregorianCalendar gregorianCalendar = xmlGregorianCalendar.toGregorianCalendar();

        java.time.Instant instant = gregorianCalendar.toZonedDateTime().toInstant();
        ZoneId zoneId = gregorianCalendar.getTimeZone().toZoneId();

        // Create ZonedDateTime using Instant and ZoneId
        return ZonedDateTime.ofInstant(instant, zoneId).withZoneSameLocal(this.zoneId);
    }

    /**
     * Maps an EDA consumption record to a CIM consumption record
     *
     * @param externalConsumptionRecord The external consumption record to map
     * @param permissionId              The permissionId to set on the mapped consumption record
     * @param connectionId              The connectionId to set on the mapped consumption record
     * @return a CIM consumption record
     */
    public energy.eddie.api.v0.ConsumptionRecord mapToCIM(ConsumptionRecord externalConsumptionRecord, @Nullable String permissionId, @Nullable String connectionId) throws InvalidMappingException {
        requireNonNull(externalConsumptionRecord);

        var consumptionRecord = new energy.eddie.api.v0.ConsumptionRecord();
        consumptionRecord.setPermissionId(permissionId); // permissionId is optional so setting null is ok
        consumptionRecord.setConnectionId(connectionId); // connectionId is optional so setting null is ok

        var crEnergy = externalConsumptionRecord.getProcessDirectory().getEnergy().stream().findFirst().orElseThrow(() -> new InvalidMappingException("No Energy found in ProcessDirectory of ConsumptionRecord"));
        consumptionRecord.setMeteringPoint(externalConsumptionRecord.getProcessDirectory().getMeteringPoint());
        consumptionRecord.setStartDateTime(toZonedDateTime(crEnergy.getMeteringPeriodStart()));
        consumptionRecord.setMeteringInterval(switch (crEnergy.getMeteringIntervall()) {
            case D -> energy.eddie.api.v0.ConsumptionRecord.MeteringInterval.PT_1_D;
            case QH -> energy.eddie.api.v0.ConsumptionRecord.MeteringInterval.PT_15_M;
            default ->
                    throw new IllegalStateException("Unexpected value: " + crEnergy.getMeteringIntervall()); // according to the schema documentation, EnergyData can only ever have D or QH as MeteringInterval https://www.ebutilities.at/schemas/149 look for the `datantypen.pdf
        });

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

            if (Objects.equals(energyPosition.getMM(), "L1")) {
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
