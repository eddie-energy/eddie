package energy.eddie.aiida.models.monitoring.openslo.sli;

import energy.eddie.aiida.models.monitoring.openslo.slo.SloMetadata;

@SuppressWarnings("NullAway")
public class SliDocument {
    private String apiVersion;
    private String kind;
    private SloMetadata metadata;
    private SliSpec spec;

    public SliDocument() {

    }

    public SliDocument(String apiVersion, String kind, SloMetadata metadata, SliSpec spec) {
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

    public SliSpec getSpec() {
        return spec;
    }

    public void setSpec(SliSpec spec) {
        this.spec = spec;
    }
}
