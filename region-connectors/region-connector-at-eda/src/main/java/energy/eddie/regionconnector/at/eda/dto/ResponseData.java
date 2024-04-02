package energy.eddie.regionconnector.at.eda.dto;

import java.util.List;

public interface ResponseData {
    String consentId();

    String meteringPoint();

    List<Integer> responseCodes();
}
