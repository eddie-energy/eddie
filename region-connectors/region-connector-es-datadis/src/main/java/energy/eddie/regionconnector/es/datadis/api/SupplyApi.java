package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SupplyApi {
    Mono<List<Supply>> getSupplies(String authorizedNif, @Nullable String distributorCode);
}