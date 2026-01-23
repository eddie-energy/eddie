// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "PermissionMarketDocuments")
public class PermissionMarketDocuments extends CimCollection<PermissionEnvelope> {
    public PermissionMarketDocuments(List<PermissionEnvelope> docs) {
        super(docs);
    }

    // Required by Jaxb2
    @SuppressWarnings("unused")
    public PermissionMarketDocuments() {
        super();
    }
}
