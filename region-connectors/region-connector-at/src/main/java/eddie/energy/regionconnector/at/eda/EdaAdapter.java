package eddie.energy.regionconnector.at.eda;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import eddie.energy.regionconnector.api.v0.ConsumptionRecordPublisher;
import eddie.energy.regionconnector.at.models.CMRequestStatus;
import jakarta.xml.bind.JAXBException;

import java.util.concurrent.Flow;


public interface EdaAdapter extends ConsumptionRecordPublisher, AutoCloseable {

    void subscribeToCMRequestStatusPublisher(Flow.Subscriber<CMRequestStatus> subscriber);


    void sendCMRequest(CMRequest request) throws TransmissionException, JAXBException;

    void start() throws TransmissionException;

}



