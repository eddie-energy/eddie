package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32;

import energy.eddie.regionconnector.at.eda.dto.masterdata.Address;
import energy.eddie.regionconnector.at.eda.dto.masterdata.ContractPartner;
import energy.eddie.regionconnector.at.eda.dto.masterdata.InvoiceRecipient;

public record InvoiceRecipient01p32(
        at.ebutilities.schemata.customerprocesses.masterdata._01p32.InvoiceRecipient invoiceRecipient) implements InvoiceRecipient {
    @Override
    public ContractPartner contractPartner() {
        return new ContractPartner01p32(invoiceRecipient.getPartnerData());
    }

    @Override
    public Address address() {
        return new Address01p32(invoiceRecipient.getAddressData());
    }
}
