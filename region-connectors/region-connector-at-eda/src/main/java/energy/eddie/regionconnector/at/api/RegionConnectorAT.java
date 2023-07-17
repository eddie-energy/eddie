package energy.eddie.regionconnector.at.api;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.requests.InvalidDsoIdException;
import jakarta.xml.bind.JAXBException;

public interface RegionConnectorAT extends RegionConnector {


    /**
     * Sends a CCMORequest to EDA (Energiewirtschaftlicher Datenaustausch), to request energy data for an austrian metering point.
     * Adheres to the process described on this <a href="https://www.ebutilities.at/prozesse/321">page</a>.
     * Updates on the status of a request can be received by subscribing to the CMRequestStatusPublisher.
     *
     * @param connectionId the id of the user at the eligible party, this can be used to identify and group permissions for a user.
     * @param request      the request to send
     * @return A SendCCMORequestResult containing a PermissionID and CMRequestId.
     * The CMRequestId can be used by the customer to identify and accept the request in the portal of his DSO.
     * The PermissionID can be used to identify which StatusUpdates belong to this request.
     * @throws TransmissionException if the request could not be sent to EDA
     */
    SendCCMORequestResult sendCCMORequest(String connectionId, CMRequest request) throws TransmissionException, InvalidDsoIdException, JAXBException;

}

