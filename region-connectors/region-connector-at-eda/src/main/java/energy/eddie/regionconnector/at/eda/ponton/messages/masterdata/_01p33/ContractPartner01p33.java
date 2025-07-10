package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p33;

import at.ebutilities.schemata.customerprocesses.masterdata._01p33.NameC;
import energy.eddie.regionconnector.at.eda.dto.masterdata.ContractPartner;
import jakarta.annotation.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.stream.Stream;

public record ContractPartner01p33(
        at.ebutilities.schemata.customerprocesses.masterdata._01p33.ContractPartner contractPartner) implements ContractPartner {
    @Override
    @Nullable
    public String salutation() {
        return contractPartner.getSalutation();
    }

    @Override
    @Nullable
    public String surname() {
        return getName(contractPartner.getName1());
    }

    @Override
    @Nullable
    public String firstName() {
        return getName(contractPartner.getName2());
    }

    @Nullable
    @Override
    public String companyName() {
        return Stream.of(
                             getName(contractPartner.getName1()),
                             getName(contractPartner.getName2())
                     )
                     .filter(name -> name != null && !name.isBlank())
                     .reduce((a, b) -> a + " " + b)
                     .orElse(null);
    }

    @Override
    @Nullable
    public String contractPartnerNumber() {
        return contractPartner.getContractPartnerNumber();
    }

    @Override
    @Nullable
    public XMLGregorianCalendar dateOfBirth() {
        return contractPartner.getDateOfBirth();
    }

    @Override
    @Nullable
    public XMLGregorianCalendar dateOfDeath() {
        return contractPartner.getDateOfDeath();
    }

    @Override
    @Nullable
    public String companyRegisterNumber() {
        return contractPartner.getCompanyRegistryNo();
    }

    @Override
    @Nullable
    public String vatNumber() {
        return contractPartner.getVATNumber();
    }

    @Override
    @Nullable
    public String email() {
        return contractPartner.getEmail();
    }

    @Nullable
    private String getName(NameC name) {
        return name != null ? name.getValue() : null;
    }
}
