// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

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
