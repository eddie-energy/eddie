package energy.eddie.regionconnector.at.eda.requests.restricted.enums;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;

public enum AllowedMeteringIntervalType {
    QH(MeteringIntervallType.QH),
    D(MeteringIntervallType.D);

    private final MeteringIntervallType value;

    AllowedMeteringIntervalType(MeteringIntervallType value) {
        this.value = value;
    }

    public MeteringIntervallType value() {
        return value;
    }
}
