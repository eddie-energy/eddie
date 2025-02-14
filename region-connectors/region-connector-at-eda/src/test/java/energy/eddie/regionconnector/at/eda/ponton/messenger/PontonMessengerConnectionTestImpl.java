package energy.eddie.regionconnector.at.eda.ponton.messenger;

import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.OutboundMessageStatusUpdateHandler;
import de.ponton.xp.adapter.api.TransmissionException;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;

import java.time.ZonedDateTime;
import java.util.Map;

public class PontonMessengerConnectionTestImpl implements PontonMessengerConnection {

    public OutboundMessageStatusUpdateHandler outboundMessageStatusUpdateHandler;
    public CMNotificationHandler cmNotificationHandler;
    public CMRevokeHandler cmRevokeHandler;
    public ConsumptionRecordHandler consumptionRecordHandler;
    public MasterDataHandler masterDataHandler;
    public CPNotificationHandler cpNotificationHandler;

    private boolean throwTransmissionException = false;
    private boolean throwConnectionException = false;
    private MessengerStatus messengerStatus = new MessengerStatus(Map.of(), true);

    public void setThrowTransmissionException(boolean throwTransmissionException) {
        this.throwTransmissionException = throwTransmissionException;
    }

    public void setThrowConnectionException(boolean throwConnectionException) {
        this.throwConnectionException = throwConnectionException;
    }

    public void setMessengerStatus(MessengerStatus messengerStatus) {
        this.messengerStatus = messengerStatus;
    }

    @Override
    public MessengerStatus messengerStatus() {
        return messengerStatus;
    }

    @Override
    public void close() {
        // No-Op
    }

    @Override
    public void start() throws TransmissionException {
        if (throwTransmissionException) {
            throw new TransmissionException("TransmissionException");
        }
    }

    @Override
    public void sendCMRevoke(CCMORevoke ccmoRevoke) throws TransmissionException, ConnectionException {
        if (throwTransmissionException) {
            throw new TransmissionException("TransmissionException");
        }
        if (throwConnectionException) {
            throw new ConnectionException("ConnectionException", new Throwable());
        }
    }

    @Override
    public void sendCMRequest(CCMORequest ccmoRequest) throws TransmissionException, ConnectionException {
        if (throwTransmissionException) {
            throw new TransmissionException("TransmissionException");
        }
        if (throwConnectionException) {
            throw new ConnectionException("ConnectionException", new Throwable());
        }
    }

    @Override
    public void sendCPRequest(CPRequestCR cpRequestCR) throws TransmissionException, ConnectionException {
        if (throwTransmissionException) {
            throw new TransmissionException("TransmissionException");
        }
        if (throwConnectionException) {
            throw new ConnectionException("ConnectionException", new Throwable());
        }
    }

    @Override
    public PontonMessengerConnection withOutboundMessageStatusUpdateHandler(OutboundMessageStatusUpdateHandler outboundMessageStatusUpdateHandler) {
        this.outboundMessageStatusUpdateHandler = outboundMessageStatusUpdateHandler;
        return this;
    }

    @Override
    public PontonMessengerConnection withCMNotificationHandler(CMNotificationHandler cmNotificationHandler) {
        this.cmNotificationHandler = cmNotificationHandler;
        return this;
    }

    @Override
    public PontonMessengerConnection withCMRevokeHandler(CMRevokeHandler cmRevokeHandler) {
        this.cmRevokeHandler = cmRevokeHandler;
        return this;
    }

    @Override
    public PontonMessengerConnection withConsumptionRecordHandler(ConsumptionRecordHandler consumptionRecordHandler) {
        this.consumptionRecordHandler = consumptionRecordHandler;
        return this;
    }

    @Override
    public PontonMessengerConnection withMasterDataHandler(MasterDataHandler masterDataHandler) {
        this.masterDataHandler = masterDataHandler;
        return this;
    }

    @Override
    public PontonMessengerConnection withCPNotificationHandler(CPNotificationHandler cpNotificationHandler) {
        this.cpNotificationHandler = cpNotificationHandler;
        return this;
    }

    @Override
    public void resendFailedMessage(ZonedDateTime date, String messageId) {
        // empty
    }
}
