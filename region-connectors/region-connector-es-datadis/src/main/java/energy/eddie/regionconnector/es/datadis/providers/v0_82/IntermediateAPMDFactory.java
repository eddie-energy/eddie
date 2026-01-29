// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;

@Component
public class IntermediateAPMDFactory {
    private final DatadisConfiguration datadisConfig;
    private final CommonInformationModelConfiguration cimConfiguration;

    public IntermediateAPMDFactory(
            DatadisConfiguration datadisConfig,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfiguration
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
