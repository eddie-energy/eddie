package eddie.energy.regionconnector.at.models;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.*;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.eda.utils.CMRequestId;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

public class CCMORequest {
    private final String sender;
    private final String receiver;
    private final OffsetDateTime from;
    private final OffsetDateTime to;
    private final DatatypeFactory datatypeFactory = DatatypeFactory.newDefaultInstance();
    private String connectionId;
    @Nullable
    private String meteringPoint;
    private String requestDataType = "MeteringData";
    private EnergyDirection energyDirection = EnergyDirection.CONSUMPTION;
    private MeteringIntervallType meteringIntervallType = MeteringIntervallType.QH;
    private TransmissionCycle transmissionCycle = TransmissionCycle.D;

    public CCMORequest(String connectionId, String sender, String receiver, OffsetDateTime from, OffsetDateTime to) {
        this.connectionId = connectionId;
        this.sender = sender;
        this.receiver = receiver;
        if (from.isAfter(to))
            throw new IllegalArgumentException("from must be before to");

        this.from = from;
        this.to = to;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public CCMORequest withMeteringPoint(String meteringPoint) {
        this.meteringPoint = meteringPoint;
        return this;
    }

    public CCMORequest withEnergyDirection(EnergyDirection energyDirection) {
        this.energyDirection = energyDirection;
        return this;
    }

    public CCMORequest withRequestDataType(String requestDataType) {
        this.requestDataType = requestDataType;
        return this;
    }

    public MeteringIntervallType getMeteringIntervallType() {
        return meteringIntervallType;
    }

    public void setMeteringIntervallType(MeteringIntervallType meteringIntervallType) {
        this.meteringIntervallType = meteringIntervallType;
    }

    public TransmissionCycle getTransmissionCycle() {
        return transmissionCycle;
    }

    public void setTransmissionCycle(TransmissionCycle transmissionCycle) {
        this.transmissionCycle = transmissionCycle;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public CMRequest toCMRequest() {
        var request = new CMRequest();

        var sender = new RoutingAddress();
        sender.setAddressType(AddressType.EC_NUMBER);
        sender.setMessageAddress(this.sender);
        var receiver = new RoutingAddress();
        receiver.setAddressType(AddressType.EC_NUMBER);
        receiver.setMessageAddress(this.receiver);

        var marketParticipant = new MarketParticipantDirectory();
        var routingHeader = new RoutingHeader();
        routingHeader.setSender(sender);
        routingHeader.setReceiver(receiver);

        routingHeader.setDocumentCreationDateTime(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now(ZoneOffset.UTC))));
        marketParticipant.setRoutingHeader(routingHeader);
        marketParticipant.setMessageCode("ANFORDERUNG_CCMO");
        marketParticipant.setSector("01");
        marketParticipant.setDocumentMode(DocumentMode.PROD);
        marketParticipant.setSchemaVersion("01.10");
        var now = Instant.now();
        var processDirectory = new ProcessDirectory();
        var messageId = sender.getMessageAddress() + "T" + now.toEpochMilli();
        var requestId = new CMRequestId(messageId);

        processDirectory.setMessageId(messageId);
        processDirectory.setConversationId(sender.getMessageAddress() + "T" + now.toEpochMilli());
        processDirectory.setProcessDate(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now(ZoneOffset.UTC))));
        if (!meteringPoint.isBlank()) processDirectory.setMeteringPoint(meteringPoint);
        var requestType = new ReqType();
        requestType.setDateFrom(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(from.toZonedDateTime())));
        requestType.setDateTo(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(to.toZonedDateTime())));
        requestType.setReqDatType(requestDataType);
        if (energyDirection != null) {
            requestType.setEnergyDirection(energyDirection);
        }
        requestType.setMeteringIntervall(this.meteringIntervallType);
        requestType.setTransmissionCycle(this.transmissionCycle);
        processDirectory.setCMRequest(requestType);
        processDirectory.setCMRequestId(requestId.toString());

        request.setProcessDirectory(processDirectory);
        request.setMarketParticipantDirectory(marketParticipant);
        return request;
    }
}

