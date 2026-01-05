package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

/**
 * Client for the ETA Plus API.
 * This client is responsible for fetching validated historical data from the German ETA Plus system.
 * TODO: Replace the stub implementation with actual ETA Plus API calls once the API specification is available.
 */
@Component
public class EtaPlusApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaPlusApiClient.class);

    private final WebClient webClient;

    public EtaPlusApiClient(WebClient deEtaWebClient) {
        this.webClient = deEtaWebClient;
    }

    /**
     * Fetch validated historical metered data for a permission request.
     * 
     * @param permissionRequest the permission request containing connection details
     * @return a Mono emitting the metered data or an error
     */
    public Mono<EtaPlusMeteredData> fetchMeteredData(DePermissionRequest permissionRequest) {
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .addArgument(permissionRequest::meteringPointId)
              .addArgument(permissionRequest::start)
              .addArgument(permissionRequest::end)
              .log("Fetching metered data for permission request {} with metering point {} from {} to {}");

        // TODO: Implement actual ETA Plus API call
        // The actual implementation should:
        // 1. Construct the API request with authentication
        // 2. Call the ETA Plus endpoint for validated historical data
        // 3. Parse the response into EtaPlusMeteredData

        // Stub implementation returning empty data for now
        return Mono.fromCallable(() -> createStubResponse(permissionRequest));
    }

    /**
     * Check if permission is still valid at ETA Plus.
     * Used to verify that the final customer hasn't revoked permission.
     *
     * @param permissionRequest the permission request to check
     * @return a Mono emitting true if permission is valid, false otherwise
     */
    public Mono<Boolean> checkPermissionValidity(DePermissionRequest permissionRequest) {
        LOGGER.atDebug()
              .addArgument(permissionRequest::permissionId)
              .log("Checking permission validity for permission request {}");

        // TODO: Implement actual permission validity check
        return Mono.just(true);
    }

    private EtaPlusMeteredData createStubResponse(DePermissionRequest permissionRequest) {
        // Stub data - replace with actual API response parsing
        LocalDate start = permissionRequest.start();
        LocalDate end = permissionRequest.end();
        
        // Don't return data for future dates
        LocalDate today = LocalDate.now();
        LocalDate effectiveEnd = end.isAfter(today) ? today : end;

        return new EtaPlusMeteredData(
                permissionRequest.meteringPointId(),
                start,
                effectiveEnd,
                List.of(
                        new EtaPlusMeteredData.MeterReading(
                                start.atStartOfDay().toString(),
                                0.0,
                                "kWh",
                                "VALIDATED"
                        )
                ),
                "{\"stub\": true, \"meteringPointId\": \"" + permissionRequest.meteringPointId() + "\"}"
        );
    }
}

