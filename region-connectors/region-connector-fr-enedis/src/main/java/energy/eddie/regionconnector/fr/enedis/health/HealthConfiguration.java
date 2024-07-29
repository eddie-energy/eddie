package energy.eddie.regionconnector.fr.enedis.health;

import energy.eddie.regionconnector.fr.enedis.api.EnedisHealth;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfiguration {
    @Bean(name = "enedisContractApiHealthIndicator")
    public EnedisApiHealthIndicator enedisContractApiHealthIndicator(EnedisHealth enedisHealth) {
        return new EnedisApiHealthIndicator(enedisHealth, EnedisApiClient.CONTRACT_API);
    }

    @Bean(name = "enedisAuthenticationApiHealthIndicator")
    public EnedisApiHealthIndicator enedisAuthenticationApiHealthIndicator(EnedisHealth enedisHealth) {
        return new EnedisApiHealthIndicator(enedisHealth, EnedisApiClient.AUTHENTICATION_API);
    }

    @Bean(name = "enedisMeteringPointApiHealthIndicator")
    public EnedisApiHealthIndicator enedisMeteringPointApiHealthIndicator(EnedisHealth enedisHealth) {
        return new EnedisApiHealthIndicator(enedisHealth, EnedisApiClient.METERING_POINT_API);
    }
}
