// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import org.springframework.stereotype.Component;

@Component
public class IntermediateAccountingPointMarketDocumentFactory {

    private final CommonInformationModelConfiguration cimConfiguration;

    public IntermediateAccountingPointMarketDocumentFactory(
            CommonInformationModelConfiguration cimConfiguration
    ) {
        this.cimConfiguration = cimConfiguration;
    }

    public IntermediateAccountingPointMarketDocument create(IdentifiableMasterData masterData) {
        return new IntermediateAccountingPointMarketDocument(masterData, cimConfiguration);
    }
}
