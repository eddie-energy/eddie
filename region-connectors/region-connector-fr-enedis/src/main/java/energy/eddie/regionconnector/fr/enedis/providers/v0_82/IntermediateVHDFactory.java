package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.springframework.stereotype.Component;

@Component
public class IntermediateVHDFactory {
    private final EnedisConfiguration enedisConfiguration;
    private final CommonInformationModelConfiguration cimConfigruation;

    public IntermediateVHDFactory(EnedisConfiguration enedisConfiguration,
                                  CommonInformationModelConfiguration cimConfigruation) {
        this.enedisConfiguration = enedisConfiguration;
        this.cimConfigruation = cimConfigruation;
    }

    public IntermediateValidatedHistoricalDocument create(IdentifiableMeterReading meterReading) {
        return new IntermediateValidatedHistoricalDocument(meterReading, cimConfigruation, enedisConfiguration);
    }
}