package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ResponseDataType;
import energy.eddie.regionconnector.at.eda.dto.ResponseData;

import java.util.List;

public record ResponseData01p11(ResponseDataType responseData) implements ResponseData {
    @Override
    public String consentId() {
        return responseData.getConsentId();
    }

    @Override
    public String meteringPoint() {
        return responseData.getMeteringPoint();
    }

    @Override
    public List<Integer> responseCodes() {
        return responseData.getResponseCode();
    }
}
