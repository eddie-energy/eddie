package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.springframework.stereotype.Component;

@Component
public class IntermediateVHDFactory {
    private final DatadisConfig datadisConfig;
    private final CommonInformationModelConfiguration cimConfiguration;

    public IntermediateVHDFactory(DatadisConfig datadisConfig,
                                  CommonInformationModelConfiguration cimConfiguration) {
        this.datadisConfig = datadisConfig;
        this.cimConfiguration = cimConfiguration;
    }

    public IntermediateValidatedHistoricalDocument create(IdentifiableMeteringData meteringData) {
        return new IntermediateValidatedHistoricalDocument(meteringData, cimConfiguration, datadisConfig);
    }
}
