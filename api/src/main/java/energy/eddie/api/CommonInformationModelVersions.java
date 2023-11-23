package energy.eddie.api;

public enum CommonInformationModelVersions {
    V0_82("0.82");

    final String version;

    CommonInformationModelVersions(String version) {
        this.version = version;
    }

    public String version() {
        return version;
    }
}