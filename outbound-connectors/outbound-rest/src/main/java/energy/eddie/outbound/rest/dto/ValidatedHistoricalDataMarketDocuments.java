// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "ValidatedHistoricalDataMarketDocuments")
public class ValidatedHistoricalDataMarketDocuments extends CimCollection<ValidatedHistoricalDataEnvelope> {
    public ValidatedHistoricalDataMarketDocuments(List<ValidatedHistoricalDataEnvelope> docs) {
        super(docs);
    }

    // Required by Jaxb2
    @SuppressWarnings("unused")
    public ValidatedHistoricalDataMarketDocuments() {
        super();
    }
}
