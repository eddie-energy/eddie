package energy.eddie.regionconnector.es.datadis.permission.request;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing the distributor codes as defined in the Datadis API.
 * Distributor codes can be used to identify the DSO of a metering point.
 * Mapping from Datadis API documentation (<a href="https://datadis.es/private-api">private-api</a>):
 * 1: Viesgo, 2: E-distribución, 3: E-redes, 4: ASEME, 5: UFD, 6: EOSA, 7: CIDE, 8: IDE.
 */
public enum DistributorCode {
    VIESGO("1", "Viesgo"),
    E_DISTRIBUCION("2", "E-distribución"),
    E_REDES("3", "E-redes"),
    ASEME("4", "ASEME"),
    UFD("5", "UFD"),
    EOSA("6", "EOSA"),
    CIDE("7", "CIDE"),
    IDE("8", "IDE");

    private static final Map<String, DistributorCode> BY_CODE = new HashMap<>();

    static {
        for (DistributorCode e : values()) {
            BY_CODE.put(e.code, e);
        }
    }

    private final String code;
    private final String name;

    DistributorCode(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static DistributorCode fromCode(String code) {
        if (BY_CODE.containsKey(code)) {
            return BY_CODE.get(code);
        }
        throw new IllegalArgumentException("Unknown distributor code: " + code);
    }

    @Override
    public String toString() {
        return name;
    }

    public String getCode() {
        return code;
    }
}