package energy.eddie.regionconnector.cds.providers.cim;

import java.util.List;

public record ServicePoint(String servicePointAddress, List<Meter> meterDevices) {
}
