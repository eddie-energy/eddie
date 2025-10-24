package energy.eddie.regionconnector.at.eda.dto;

import java.time.LocalDate;

public interface EdaCMRevoke {
    String meteringPoint();

    String consentId();

    LocalDate consentEnd();
}
