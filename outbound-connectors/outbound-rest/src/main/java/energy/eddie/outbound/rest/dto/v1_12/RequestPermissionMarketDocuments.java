// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto.v1_12;

import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import energy.eddie.outbound.rest.dto.CimCollection;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "RequestPermissionMarketDocuments")
public class RequestPermissionMarketDocuments extends CimCollection<RequestPermissionEnvelope> {
    public RequestPermissionMarketDocuments(List<RequestPermissionEnvelope> docs) {
        super(docs);
    }

    // Required by Jaxb2
    @SuppressWarnings("unused")
    public RequestPermissionMarketDocuments() {
        super();
    }
}
