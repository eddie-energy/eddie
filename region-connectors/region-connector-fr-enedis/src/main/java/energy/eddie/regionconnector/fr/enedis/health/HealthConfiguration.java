package energy.eddie.regionconnector.fr.enedis.health;

import energy.eddie.regionconnector.fr.enedis.api.EnedisHealth;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfiguration {
    @Bean(name = "enedisAuthenticationApiHealthIndicator")
    public EnedisApiHealthIndicator enedisAuthenticationApiHealthIndicator(EnedisHealth enedisHealth) {
        return new EnedisApiHealthIndicator(enedisHealth, EnedisApiClient.AUTHENTICATION_API);
    }

    @Bean(name = "enedisMeteringPointApiHealthIndicator")
    public EnedisApiHealthIndicator enedisMeteringPointApiHealthIndicator(EnedisHealth enedisHealth) {
        return new EnedisApiHealthIndicator(enedisHealth, EnedisApiClient.METERING_POINT_API);
    }

    @Bean(name = "enedisContractApiHealthIndicator")
    public EnedisApiHealthIndicator enedisContractApiHealthIndicator(EnedisHealth enedisHealth) {
        return new EnedisApiHealthIndicator(enedisHealth, EnedisApiClient.CONTRACT_API);
    }

    @Bean(name = "enedisContactApiHealthIndicator")
    public EnedisApiHealthIndicator enedisContactApiHealthIndicator(EnedisHealth enedisHealth) {
        return new EnedisApiHealthIndicator(enedisHealth, EnedisApiClient.CONTACT_API);
    }

    @Bean(name = "enedisIdentityApiHealthIndicator")
    public EnedisApiHealthIndicator enedisIdentityApiHealthIndicator(EnedisHealth enedisHealth) {
        return new EnedisApiHealthIndicator(enedisHealth, EnedisApiClient.IDENTITY_API);
    }

    @Bean(name = "enedisAddressApiHealthIndicator")
    public EnedisApiHealthIndicator enedisAddressApiHealthIndicator(EnedisHealth enedisHealth) {
        return new EnedisApiHealthIndicator(enedisHealth, EnedisApiClient.ADDRESS_API);
    }
}
