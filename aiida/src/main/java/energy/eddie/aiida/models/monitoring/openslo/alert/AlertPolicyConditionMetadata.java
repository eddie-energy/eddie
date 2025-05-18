package energy.eddie.aiida.models.monitoring.openslo.alert;

@SuppressWarnings("NullAway")
public class AlertPolicyConditionMetadata {
    private String name;
    private String displayName;

    public AlertPolicyConditionMetadata() {

    }

    public AlertPolicyConditionMetadata(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
