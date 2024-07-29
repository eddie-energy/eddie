package energy.eddie.regionconnector.at.eda.dto;

import java.util.List;

public record SimpleResponseData(
        String consentId,
        String meteringPoint,
        List<Integer> responseCodes
)
        implements ResponseData {
}
