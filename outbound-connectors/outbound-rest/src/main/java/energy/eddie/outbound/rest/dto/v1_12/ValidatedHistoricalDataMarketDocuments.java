// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto.v1_12;

import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.outbound.rest.dto.CimCollection;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlType(name = "ValidatedHistoricalDataMarketDocumentsV1_12")
@XmlRootElement(name = "ValidatedHistoricalDataMarketDocuments")
public class ValidatedHistoricalDataMarketDocuments extends CimCollection<Envelope> {
    public ValidatedHistoricalDataMarketDocuments(List<Envelope> docs) {
        super(docs);
    }

    // Required by Jaxb2
    @SuppressWarnings("unused")
    public ValidatedHistoricalDataMarketDocuments() {
        super();
    }
}
