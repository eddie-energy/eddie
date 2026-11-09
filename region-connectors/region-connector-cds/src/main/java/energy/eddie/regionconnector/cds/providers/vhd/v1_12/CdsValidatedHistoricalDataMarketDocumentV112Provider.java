// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.cds.providers.vhd.v1_04.CdsValidatedHistoricalDataMarketDocumentV104Provider;
import energy.eddie.regionconnector.shared.cim.v1_12.ValidatedHistoricalDataV112Converter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component("cdsValidatedHistoricalDataMarketDocumentV112Provider")
public class CdsValidatedHistoricalDataMarketDocumentV112Provider {
    private final Flux<Envelope> validatedHistoricalData;

    public CdsValidatedHistoricalDataMarketDocumentV112Provider(CdsValidatedHistoricalDataMarketDocumentV104Provider provider) {
        this.validatedHistoricalData = provider.getValidatedHistoricalDataMarketDocumentsStream()
                .map(ValidatedHistoricalDataV112Converter::convert);
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataV112Stream() {
        return validatedHistoricalData;
    }
}
