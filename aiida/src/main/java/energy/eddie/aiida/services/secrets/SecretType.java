package energy.eddie.aiida.services.secrets;

import java.util.Locale;

public enum SecretType {
    PASSWORD,
    API_KEY;

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
