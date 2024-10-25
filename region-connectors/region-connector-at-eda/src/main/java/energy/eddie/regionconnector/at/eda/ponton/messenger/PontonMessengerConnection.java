package energy.eddie.regionconnector.at.eda.ponton.messenger;

import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.OutboundMessageStatusUpdateHandler;
import de.ponton.xp.adapter.api.TransmissionException;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;

public interface PontonMessengerConnection extends MessengerHealth, MessengerMonitor {
    static PontonMessengerConnectionBuilder newBuilder() {
        return new PontonMessengerConnectionBuilder();
    }

    void close();

    void start() throws TransmissionException;

    void sendCMRevoke(CCMORevoke ccmoRevoke) throws TransmissionException, ConnectionException;

    void sendCMRequest(CCMORequest ccmoRequest) throws TransmissionException, ConnectionException;

    void sendCPRequest(CPRequestCR cpRequestCR) throws TransmissionException, ConnectionException;

    PontonMessengerConnection withOutboundMessageStatusUpdateHandler(OutboundMessageStatusUpdateHandler outboundMessageStatusUpdateHandler);

    PontonMessengerConnection withCMNotificationHandler(CMNotificationHandler cmNotificationHandler);

    PontonMessengerConnection withCMRevokeHandler(CMRevokeHandler cmRevokeHandler);

    PontonMessengerConnection withConsumptionRecordHandler(ConsumptionRecordHandler consumptionRecordHandler);

    PontonMessengerConnection withMasterDataHandler(MasterDataHandler masterDataHandler);

    PontonMessengerConnection withCPNotificationHandler(CPNotificationHandler cpNotificationHandler);
}
