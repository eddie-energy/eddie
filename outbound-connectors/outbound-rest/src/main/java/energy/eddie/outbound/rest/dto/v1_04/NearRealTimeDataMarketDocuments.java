// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto.v1_04;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.dto.CimCollection;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlType(name = "NearRealTimeDataMarketDocumentsV1_04")
@XmlRootElement(name = "NearRealTimeDataMarketDocuments")
public class NearRealTimeDataMarketDocuments extends CimCollection<RTDEnvelope> {
    public NearRealTimeDataMarketDocuments(List<RTDEnvelope> docs) {
        super(docs);
    }

    // Required by Jaxb2
    @SuppressWarnings("unused")
    public NearRealTimeDataMarketDocuments() {
        super();
    }
}
