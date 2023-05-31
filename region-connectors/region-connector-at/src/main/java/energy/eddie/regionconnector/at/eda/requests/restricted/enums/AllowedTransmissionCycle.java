package energy.eddie.regionconnector.at.eda.requests.restricted.enums;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.TransmissionCycle;

public enum AllowedTransmissionCycle {
    D(TransmissionCycle.D),
    M(TransmissionCycle.M);

    private final TransmissionCycle value;

    AllowedTransmissionCycle(TransmissionCycle value) {
        this.value = value;
    }

    public TransmissionCycle value() {
        return value;
    }
}
