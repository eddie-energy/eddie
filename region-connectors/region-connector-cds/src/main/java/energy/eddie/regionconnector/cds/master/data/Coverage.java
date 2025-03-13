package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Locale;

@Embeddable
public class Coverage {
    @Enumerated(EnumType.STRING)
    @Column(name = "energy_type", nullable = false)
    private final EnergyType energyType;
    @Column(name = "country_code", nullable = false)
    private final String countryCode;

    public Coverage(EnergyType energyType, String countryCode) {
        this.energyType = energyType;
        this.countryCode = countryCode;
    }

    @SuppressWarnings("NullAway")
    protected Coverage() {
        this(null, null);
    }

    public String countryCode() {
        return countryCode.toLowerCase(Locale.ROOT);
    }

    public EnergyType energyType() {
        return energyType;
    }

    @Override
    public int hashCode() {
        int result = energyType.hashCode();
        result = 31 * result + countryCode.hashCode();
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Coverage coverage)) return false;

        return energyType == coverage.energyType && countryCode.equalsIgnoreCase(coverage.countryCode);
    }
}
