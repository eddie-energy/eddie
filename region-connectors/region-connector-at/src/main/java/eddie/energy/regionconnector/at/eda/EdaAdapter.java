package eddie.energy.regionconnector.at.eda;

import eddie.energy.regionconnector.api.v0.ConsumptionRecordPublisher;
import eddie.energy.regionconnector.at.models.CCMORequest;
import eddie.energy.regionconnector.at.models.CMRequestStatus;
import jakarta.xml.bind.JAXBException;

import java.util.concurrent.Flow;


public interface EdaAdapter extends ConsumptionRecordPublisher, AutoCloseable {

    void subscribeToCMRequestStatusPublisher(Flow.Subscriber<CMRequestStatus> subscriber);

    void sendCMRequest(CCMORequest request) throws TransmissionException, JAXBException;

    void start() throws TransmissionException;

}



