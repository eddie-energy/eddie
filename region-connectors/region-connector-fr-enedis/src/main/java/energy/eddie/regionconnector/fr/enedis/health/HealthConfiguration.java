package energy.eddie.regionconnector.fr.enedis.health;

import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfiguration {
    @Bean(name = "enedisContractApiHealthIndicator")
    public EnedisApiHealthIndicator enedisContractApiHealthIndicator(EnedisApi enedisApi) {
        return new EnedisApiHealthIndicator(enedisApi, EnedisApiClient.CONTRACT_API);
    }

    @Bean(name = "enedisAuthenticationApiHealthIndicator")
    public EnedisApiHealthIndicator enedisAuthenticationApiHealthIndicator(EnedisApi enedisApi) {
        return new EnedisApiHealthIndicator(enedisApi, EnedisApiClient.AUTHENTICATION_API);
    }

    @Bean(name = "enedisMeteringPointApiHealthIndicator")
    public EnedisApiHealthIndicator enedisMeteringPointApiHealthIndicator(EnedisApi enedisApi) {
        return new EnedisApiHealthIndicator(enedisApi, EnedisApiClient.METERING_POINT_API);
    }
}
