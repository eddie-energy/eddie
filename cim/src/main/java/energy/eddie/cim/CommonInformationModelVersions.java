package energy.eddie.cim;

/**
 * The supported versions of the CIM
 */
public enum CommonInformationModelVersions {
    V0_82("0.82"),
    V0_91_08("0.91.08"),
    V1_04("1.04");

    private final String version;

    CommonInformationModelVersions(String version) {
        this.version = version;
    }

    public String version() {
        return version;
    }

    public String cimify() {
        return version.replace(".", "");
    }
}
