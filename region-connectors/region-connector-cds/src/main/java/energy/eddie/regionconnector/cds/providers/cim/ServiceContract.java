package energy.eddie.regionconnector.cds.providers.cim;

import java.util.List;

public record ServiceContract(String contractAddress, String serviceType, List<ServicePoint> servicePoints) {
}
