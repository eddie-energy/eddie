package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;

@Component
public class IntermediateAPMDFactory {
    private final DatadisConfig datadisConfig;
    private final CommonInformationModelConfiguration cimConfiguration;

    public IntermediateAPMDFactory(
            DatadisConfig datadisConfig,
            CommonInformationModelConfiguration cimConfiguration
    ) {
        this.datadisConfig = datadisConfig;
        this.cimConfiguration = cimConfiguration;
    }

    public IntermediateAccountingPointMarketDocument create(IdentifiableAccountingPointData identifiableAccountingPointData) {
        return new IntermediateAccountingPointMarketDocument(identifiableAccountingPointData,
                                                             cimConfiguration,
                                                             datadisConfig);
    }
}
