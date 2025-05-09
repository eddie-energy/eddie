package energy.eddie.aiida.models.monitoring.openslo.slo;

@SuppressWarnings("NullAway")
public class SloDocument {

    private String apiVersion;
    private String kind;
    private SloMetadata metadata;
    private SloSpec spec;

    public SloDocument() {
    }

    public SloDocument(String apiVersion, String kind, SloMetadata metadata, SloSpec spec) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.metadata = metadata;
        this.spec = spec;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public SloMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(SloMetadata metadata) {
        this.metadata = metadata;
    }

    public SloSpec getSpec() {
        return spec;
    }

    public void setSpec(SloSpec spec) {
        this.spec = spec;
    }
}
