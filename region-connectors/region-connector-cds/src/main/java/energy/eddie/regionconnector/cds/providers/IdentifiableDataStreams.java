package energy.eddie.regionconnector.cds.providers;

import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.providers.vhd.IdentifiableValidatedHistoricalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

@Component
public class IdentifiableDataStreams implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableDataStreams.class);

    private final Sinks.Many<IdentifiableValidatedHistoricalData> validatedHistoricalDataSink = Sinks.many()
                                                                                                     .multicast()
                                                                                                     .onBackpressureBuffer();

    public Flux<IdentifiableValidatedHistoricalData> validatedHistoricalData() {
        return validatedHistoricalDataSink.asFlux();
    }

    @Override
    public void close() {
        validatedHistoricalDataSink.tryEmitComplete();
    }

    public void publish(
            CdsPermissionRequest pr,
            List<AccountsEndpoint200ResponseAllOfAccountsInner> accounts,
            List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner> serviceContracts,
            List<ServicePointEndpoint200ResponseAllOfServicePointsInner> servicePoints,
            List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner> meterDevices,
            List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner> usageSegments
    ) {
        LOGGER.atInfo()
              .addArgument(pr::permissionId)
              .log("Publishing data related to validated historical data for permission request {}");
        var id = new IdentifiableValidatedHistoricalData(
                pr,
                new IdentifiableValidatedHistoricalData.Payload(accounts,
                                                                serviceContracts,
                                                                servicePoints,
                                                                meterDevices,
                                                                usageSegments)
        );
        validatedHistoricalDataSink.tryEmitNext(id);
    }
}
