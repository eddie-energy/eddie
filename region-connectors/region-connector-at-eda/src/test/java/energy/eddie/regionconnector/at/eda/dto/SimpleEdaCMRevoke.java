package energy.eddie.regionconnector.at.eda.dto;

import java.time.LocalDate;

public class SimpleEdaCMRevoke implements EdaCMRevoke {

    private String meteringPoint;
    private String consentId;
    private String reason;
    private LocalDate consentEnd;

    @Override
    public String meteringPoint() {
        return meteringPoint;
    }

    @Override
    public String consentId() {
        return consentId;
    }

    @Override
    public String reason() {
        return reason;
    }

    @Override
    public LocalDate consentEnd() {
        return consentEnd;
    }

    public SimpleEdaCMRevoke setMeteringPoint(String meteringPoint) {
        this.meteringPoint = meteringPoint;
        return this;
    }

    public SimpleEdaCMRevoke setConsentId(String consentId) {
        this.consentId = consentId;
        return this;
    }

    public SimpleEdaCMRevoke setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public SimpleEdaCMRevoke setConsentEnd(LocalDate consentEnd) {
        this.consentEnd = consentEnd;
        return this;
    }
}
