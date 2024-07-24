package energy.eddie.regionconnector.at.eda.dto;

import energy.eddie.regionconnector.at.eda.dto.masterdata.*;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface EdaMasterData {

    String conversationId();

    String messageId();

    Sector sector();

    ZonedDateTime documentCreationDateTime();

    String senderMessageAddress();

    String receiverMessageAddress();

    String meteringPoint();

    MeteringPointData meteringPointData();

    Optional<BillingData> billingData();

    Optional<ContractPartner> contractPartner();

    Optional<DeliveryAddress> installationAddress();

    Optional<InvoiceRecipient> invoiceRecipient();


    Object originalMasterData();
}
