package energy.eddie.aiida.models.monitoring.openslo.alert;

import java.util.List;

@SuppressWarnings("NullAway")
public class AlertPolicySpec {
    private String description;
    private boolean alertWhenBreaching;
    private boolean alertWhenResolved;
    private boolean alertWhenNoData;
    private List<AlertPolicyCondition> conditions;

    public AlertPolicySpec() {
    }

    public AlertPolicySpec(String description, boolean alertWhenBreaching, boolean alertWhenResolved, boolean alertWhenNoData, List<AlertPolicyCondition> conditions) {
        this.description = description;
        this.alertWhenBreaching = alertWhenBreaching;
        this.alertWhenResolved = alertWhenResolved;
        this.alertWhenNoData = alertWhenNoData;
        this.conditions = conditions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAlertWhenBreaching() {
        return alertWhenBreaching;
    }

    public void setAlertWhenBreaching(boolean alertWhenBreaching) {
        this.alertWhenBreaching = alertWhenBreaching;
    }

    public boolean isAlertWhenResolved() {
        return alertWhenResolved;
    }

    public void setAlertWhenResolved(boolean alertWhenResolved) {
        this.alertWhenResolved = alertWhenResolved;
    }

    public boolean isAlertWhenNoData() {
        return alertWhenNoData;
    }

    public void setAlertWhenNoData(boolean alertWhenNoData) {
        this.alertWhenNoData = alertWhenNoData;
    }

    public List<AlertPolicyCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<AlertPolicyCondition> conditions) {
        this.conditions = conditions;
    }
}
