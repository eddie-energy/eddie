
package energy.eddie.regionconnector.de.eta.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.de.eta.DeEtaRegionConnectorMetadata;
import energy.eddie.regionconnector.shared.agnostic.JsonRawDataProvider;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import energy.eddie.regionconnector.de.eta.streams.ValidatedHistoricalDataStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {

    @Bean
    @OnRawDataMessagesEnabled
    public JsonRawDataProvider jsonRawDataProvider(
            ObjectMapper objectMapper,
            ValidatedHistoricalDataStream validatedHistoricalDataStream
    ) {
        return new JsonRawDataProvider(
                DeEtaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                objectMapper,
                validatedHistoricalDataStream.validatedHistoricalData()
        );
    }
}
