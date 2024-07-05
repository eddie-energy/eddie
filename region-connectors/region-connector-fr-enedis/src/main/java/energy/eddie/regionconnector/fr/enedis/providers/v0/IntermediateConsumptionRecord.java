package energy.eddie.regionconnector.fr.enedis.providers.v0;

import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.dto.IntervalReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;

import java.util.ArrayList;
import java.util.List;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;


class IntermediateConsumptionRecord {

    private final IdentifiableMeterReading meterReading;

    public IntermediateConsumptionRecord(IdentifiableMeterReading meterReading) {
        this.meterReading = meterReading;
    }

    public ConsumptionRecord consumptionRecord() throws IllegalStateException {
        var clcMeterReading = meterReading.meterReading();
        if (clcMeterReading.intervalReadings().isEmpty()) {
            throw new IllegalStateException("No data available");
        }
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();

        consumptionRecord.setMeteringPoint(clcMeterReading.usagePointId());
        consumptionRecord.setStartDateTime(clcMeterReading.start().atStartOfDay(ZONE_ID_FR));
        consumptionRecord.setMeteringInterval(meterReading.permissionRequest().granularity().name());


        List<ConsumptionPoint> consumptionPoints = new ArrayList<>();
        for (IntervalReading clcInterval : clcMeterReading.intervalReadings()) {
            ConsumptionPoint consumptionPoint = new ConsumptionPoint();
            consumptionPoint.setConsumption(Double.valueOf(clcInterval.value()));
            consumptionPoint.setMeteringType(
                    clcInterval.measureType()
                               .filter(measureType -> measureType.equals("B"))
                               .map(measureType -> ConsumptionPoint.MeteringType.MEASURED_VALUE)
                               .orElse(ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE)
            );

            consumptionPoints.add(consumptionPoint);
        }

        consumptionRecord.setConsumptionPoints(consumptionPoints);
        FrEnedisPermissionRequest permissionRequest = meterReading.permissionRequest();
        consumptionRecord.setPermissionId(permissionRequest.permissionId());
        consumptionRecord.setConnectionId(permissionRequest.connectionId());
        consumptionRecord.setDataNeedId(permissionRequest.dataNeedId());
        return consumptionRecord;
    }
}
