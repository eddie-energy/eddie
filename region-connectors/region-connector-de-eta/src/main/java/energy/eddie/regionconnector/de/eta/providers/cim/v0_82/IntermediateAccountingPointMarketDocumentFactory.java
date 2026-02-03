package energy.eddie.regionconnector.de.eta.providers.cim.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;

/**
 * Factory for creating IntermediateAccountingPointMarketDocument instances.
 * 
 * Aligns with EDDIE documentation:
 * - https://architecture.eddie.energy/framework/3-extending/region-connector/quickstart.html#accounting-point-data
 * - Creates intermediate documents for CIM mapping
 */
@Component
public class IntermediateAccountingPointMarketDocumentFactory {

    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration config;

    public IntermediateAccountingPointMarketDocumentFactory(
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration config
    ) {
        this.cimConfig = cimConfig;
        this.config = config;
    }

    /**
     * Creates an IntermediateAccountingPointMarketDocument from identifiable accounting point data.
     * 
     * @param identifiableAccountingPointData the identifiable accounting point data
     * @return the intermediate document
     */
    public IntermediateAccountingPointMarketDocument create(IdentifiableAccountingPointData identifiableAccountingPointData) {
        return new IntermediateAccountingPointMarketDocument(
                identifiableAccountingPointData,
                cimConfig,
                config
        );
    }
}
