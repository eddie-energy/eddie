package energy.eddie.aiida.models.monitoring.openslo.alert;

@SuppressWarnings("NullAway")
public class AlertPolicyConditionSpec {
    private String description;
    private String severity;
    private AlertPolicyConditionValue condition;

    public AlertPolicyConditionSpec() {

    }

    public AlertPolicyConditionSpec(String description, String severity, AlertPolicyConditionValue condition) {
        this.description = description;
        this.severity = severity;
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public AlertPolicyConditionValue getCondition() {
        return condition;
    }

    public void setCondition(AlertPolicyConditionValue condition) {
        this.condition = condition;
    }
}
