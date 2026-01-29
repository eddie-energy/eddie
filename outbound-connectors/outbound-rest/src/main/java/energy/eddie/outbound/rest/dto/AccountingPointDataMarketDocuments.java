// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "AccountingPointDataMarketDocuments")
public class AccountingPointDataMarketDocuments extends CimCollection<AccountingPointEnvelope> {
    public AccountingPointDataMarketDocuments(List<AccountingPointEnvelope> docs) {
        super(docs);
    }

    // Required by Jaxb2
    @SuppressWarnings("unused")
    public AccountingPointDataMarketDocuments() {
        super();
    }
}
