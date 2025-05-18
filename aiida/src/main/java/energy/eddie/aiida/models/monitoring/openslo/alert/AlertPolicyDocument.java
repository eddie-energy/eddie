package energy.eddie.aiida.models.monitoring.openslo.alert;

import energy.eddie.aiida.models.monitoring.openslo.slo.SloMetadata;

@SuppressWarnings("NullAway")
public class AlertPolicyDocument {
    private String apiVersion;
    private String kind;
    private SloMetadata metadata;
    private AlertPolicySpec spec;

    public AlertPolicyDocument() {

    }

    public AlertPolicyDocument(String apiVersion, String kind, SloMetadata metadata, AlertPolicySpec spec) {
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

    public AlertPolicySpec getSpec() {
        return spec;
    }

    public void setSpec(AlertPolicySpec spec) {
        this.spec = spec;
    }
}
