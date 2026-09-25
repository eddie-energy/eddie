// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p35;

import energy.eddie.regionconnector.at.eda.dto.masterdata.Address;
import energy.eddie.regionconnector.at.eda.dto.masterdata.ContractPartner;
import energy.eddie.regionconnector.at.eda.dto.masterdata.InvoiceRecipient;

public record InvoiceRecipient01p35(
        at.ebutilities.schemata.customerprocesses.masterdata._01p35.InvoiceRecipient invoiceRecipient) implements InvoiceRecipient {
    @Override
    public ContractPartner contractPartner() {
        return new ContractPartner01p35(invoiceRecipient.getPartnerData());
    }

    @Override
    public Address address() {
        return new Address01p35(invoiceRecipient.getAddressData());
    }
}
