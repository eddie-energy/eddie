// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p33;

import at.ebutilities.schemata.customerprocesses.masterdata._01p33.MasterData;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.dto.masterdata.*;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.NullMeteringPointData;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;

import java.time.ZonedDateTime;
import java.util.Optional;

public record EdaMasterData01p33(MasterData masterData) implements EdaMasterData {
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
    public ZonedDateTime documentCreationDateTime() {
        return XmlGregorianCalenderUtils.toUtcZonedDateTime(masterData.getMarketParticipantDirectory()
                                                                      .getRoutingHeader()
                                                                      .getDocumentCreationDateTime());
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
        var meteringPointData = masterData.getProcessDirectory().getMeteringPointData();
        return meteringPointData == null
                ? new NullMeteringPointData()
                : new MeteringPointData01p33(meteringPointData);
    }

    @Override
    public Optional<BillingData> billingData() {
        var billingData = masterData.getProcessDirectory().getBillingData();
        return billingData == null ? Optional.empty() : Optional.of(new BillingData01p33(billingData));
    }

    @Override
    public Optional<ContractPartner> contractPartner() {
        var contractPartner = masterData.getProcessDirectory().getContractPartner();
        return contractPartner == null ? Optional.empty() : Optional.of(new ContractPartner01p33(contractPartner));
    }

    @Override
    public Optional<DeliveryAddress> installationAddress() {
        var installationAddress = masterData.getProcessDirectory().getDeliveryAddress();
        return installationAddress == null ? Optional.empty() : Optional.of(new DeliveryAddress01p33(installationAddress));
    }

    @Override
    public Optional<InvoiceRecipient> invoiceRecipient() {
        var invoiceRecipient = masterData.getProcessDirectory().getInvoiceRecipient();
        return invoiceRecipient == null ? Optional.empty() : Optional.of(new InvoiceRecipient01p33(invoiceRecipient));
    }

    @Override
    public Object originalMasterData() {
        return masterData;
    }
}
