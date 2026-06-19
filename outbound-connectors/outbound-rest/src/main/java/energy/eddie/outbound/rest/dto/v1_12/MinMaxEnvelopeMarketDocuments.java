// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto.v1_12;

import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.outbound.rest.dto.CimCollection;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlType(name = "MinMaxEnvelopeMarketDocuments")
@XmlRootElement(name = "MinMaxEnvelopeMarketDocuments")
public class MinMaxEnvelopeMarketDocuments extends CimCollection<RECMMOEEnvelope> {
    public MinMaxEnvelopeMarketDocuments(List<RECMMOEEnvelope> docs) {
        super(docs);
    }

    @SuppressWarnings("unused")
    public MinMaxEnvelopeMarketDocuments() {
        super();
    }
}
