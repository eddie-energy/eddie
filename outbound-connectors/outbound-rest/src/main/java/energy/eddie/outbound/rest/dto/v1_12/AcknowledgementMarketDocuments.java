// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto.v1_12;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.outbound.rest.dto.CimCollection;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlType(name = "AcknowledgementMarketDocuments")
@XmlRootElement(name = "AcknowledgementMarketDocuments")
public class AcknowledgementMarketDocuments extends CimCollection<AcknowledgementEnvelope> {
    public AcknowledgementMarketDocuments(List<AcknowledgementEnvelope> docs) {
        super(docs);
    }

    // Required by Jaxb2
    @SuppressWarnings("unused")
    public AcknowledgementMarketDocuments() {
        super();
    }
}
