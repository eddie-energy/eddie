package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p00;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;

import java.time.LocalDate;

public record EdaCMRevoke01p00(
        CMRevoke cmRevoke
) implements EdaCMRevoke {
    @Override
    public String meteringPoint() {
        return cmRevoke.getProcessDirectory().getMeteringPoint();
    }

    @Override
    public String consentId() {
        return cmRevoke.getProcessDirectory().getConsentId();
    }

    @Override
    public String reason() {
        return cmRevoke.getProcessDirectory().getReason();
    }

    @Override
    public LocalDate consentEnd() {
        var end = cmRevoke.getProcessDirectory().getConsentEnd();
        return LocalDate.of(end.getYear(), end.getMonth(), end.getDay());
    }
}
