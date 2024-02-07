package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSupplyForMeteringPointException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public record SupplyMeteringPointFilter(List<Supply> supplies, String meteringPointId) {
    public Mono<Supply> filter() {
        if (supplies.isEmpty()) {
            return Mono.error(new NoSuppliesException("No supplies found"));
        }
        return supplies.stream()
                .filter(s -> Objects.equals(s.meteringPoint(), meteringPointId))
                .findFirst()
                .map(Mono::just)
                .orElseGet(() -> Mono.error(new NoSupplyForMeteringPointException("No supply found for metering point")));
    }
}