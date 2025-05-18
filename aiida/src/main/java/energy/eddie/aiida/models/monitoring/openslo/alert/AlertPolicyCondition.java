package energy.eddie.aiida.models.monitoring.openslo.alert;

@SuppressWarnings("NullAway")
public class AlertPolicyCondition {
    private String kind;
    private AlertPolicyConditionMetadata metadata;
    private AlertPolicyConditionSpec spec;

    public AlertPolicyCondition() {

    }

    public AlertPolicyCondition(String kind, AlertPolicyConditionMetadata metadata, AlertPolicyConditionSpec spec) {
        this.kind = kind;
        this.metadata = metadata;
        this.spec = spec;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public AlertPolicyConditionMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AlertPolicyConditionMetadata metadata) {
        this.metadata = metadata;
    }

    public AlertPolicyConditionSpec getSpec() {
        return spec;
    }

    public void setSpec(AlertPolicyConditionSpec spec) {
        this.spec = spec;
    }
}
