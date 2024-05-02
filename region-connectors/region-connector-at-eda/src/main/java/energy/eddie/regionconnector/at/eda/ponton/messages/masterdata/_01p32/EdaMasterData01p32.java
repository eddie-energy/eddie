package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32;

import at.ebutilities.schemata.customerprocesses.masterdata._01p32.MasterData;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.dto.masterdata.*;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Optional;

public record EdaMasterData01p32(
        MasterData masterData) implements EdaMasterData {
    @Override
    public String conversationId() {
        return masterData.getProcessDirectory().getConversationId();
    }

    @Override
    public String messageId() {
        return masterData.getProcessDirectory().getMessageId();
    }

    @Override
    public Sector sector() {
        return Sector.fromValue(masterData.getMarketParticipantDirectory().getSector());
    }

    @Override
    public XMLGregorianCalendar documentCreationDateTime() {
        return masterData.getMarketParticipantDirectory().getRoutingHeader().getDocumentCreationDateTime();
    }

    @Override
    public String senderMessageAddress() {
        return masterData.getMarketParticipantDirectory().getRoutingHeader().getSender().getMessageAddress();
    }

    @Override
    public String receiverMessageAddress() {
        return masterData.getMarketParticipantDirectory().getRoutingHeader().getReceiver().getMessageAddress();
    }

    @Override
    public String meteringPoint() {
        return masterData.getProcessDirectory().getMeteringPoint();
    }

    @Override
    public MeteringPointData meteringPointData() {
        return new MeteringPointData01p32(masterData.getProcessDirectory().getMeteringPointData());
    }

    @Override
    public Optional<BillingData> billingData() {
        var billingData = masterData.getProcessDirectory().getBillingData();
        return billingData == null ? Optional.empty() : Optional.of(new BillingData01p32(billingData));
    }

    @Override
    public Optional<ContractPartner> contractPartner() {
        var contractPartner = masterData.getProcessDirectory().getContractPartner();
        return contractPartner == null ? Optional.empty() : Optional.of(new ContractPartner01p32(contractPartner));
    }

    @Override
    public Optional<DeliveryAddress> installationAddress() {
        var installationAddress = masterData.getProcessDirectory().getDeliveryAddress();
        return installationAddress == null ? Optional.empty() : Optional.of(new DeliveryAddress01p32(installationAddress));
    }

    @Override
    public Optional<InvoiceRecipient> invoiceRecipient() {
        var invoiceRecipient = masterData.getProcessDirectory().getInvoiceRecipient();
        return invoiceRecipient == null ? Optional.empty() : Optional.of(new InvoiceRecipient01p32(invoiceRecipient));
    }

    @Override
    public Object originalMasterData() {
        return masterData;
    }
}
