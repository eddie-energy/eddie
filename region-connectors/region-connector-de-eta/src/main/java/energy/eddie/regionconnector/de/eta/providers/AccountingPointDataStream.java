package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AccountingPointDataStream implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDataStream.class);

    private final Sinks.Many<IdentifiableAccountingPointData> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

    private final Flux<IdentifiableAccountingPointData> flux;

    public AccountingPointDataStream() {
        this.flux = sink.asFlux().share();
    }

    /**
     * Get the flux of accounting point data.
     * 
     * @return flux of identifiable accounting point data
     */
    public Flux<IdentifiableAccountingPointData> accountingPointData() {
        return flux;
    }

    /**
     * Publish a chunk of accounting point data.
     * 
     * @param permissionRequest the permission request associated with this data
     * @param data the accounting point data from the MDA
     */
    public void publish(DePermissionRequest permissionRequest, EtaPlusAccountingPointData data) {
        var identifiableData = new IdentifiableAccountingPointData(permissionRequest, data);

        Sinks.EmitResult result = sink.tryEmitNext(identifiableData);

        if (result.isFailure()) {
            LOGGER.error("Failed to emit accounting point data for permission {}: {}", 
                    permissionRequest.permissionId(), result);
        }
    }

    @Override
    public void close() {
        sink.tryEmitComplete();
    }
}
