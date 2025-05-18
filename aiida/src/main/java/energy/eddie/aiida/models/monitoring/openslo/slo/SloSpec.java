package energy.eddie.aiida.models.monitoring.openslo.slo;

import java.util.List;

@SuppressWarnings("NullAway")
public class SloSpec {
    private String description;
    private String service;
    private String indicatorRef;
    private SloTimeWindow timeWindow;
    private String budgetingMethod;
    private SloObjective objective;
    private List<String> alertPolicies;

    public SloSpec() {
    }

    public SloSpec(String description, String service, String indicatorRef, SloTimeWindow timeWindow,
                   String budgetingMethod, SloObjective objective, List<String> alertPolicies) {
        this.description = description;
        this.service = service;
        this.indicatorRef = indicatorRef;
        this.timeWindow = timeWindow;
        this.budgetingMethod = budgetingMethod;
        this.objective = objective;
        this.alertPolicies = alertPolicies;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getIndicatorRef() {
        return indicatorRef;
    }

    public void setIndicatorRef(String indicatorRef) {
        this.indicatorRef = indicatorRef;
    }

    public SloTimeWindow getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(SloTimeWindow timeWindow) {
        this.timeWindow = timeWindow;
    }

    public String getBudgetingMethod() {
        return budgetingMethod;
    }

    public void setBudgetingMethod(String budgetingMethod) {
        this.budgetingMethod = budgetingMethod;
    }

    public SloObjective getObjective() {
        return objective;
    }

    public void setObjective(SloObjective objective) {
        this.objective = objective;
    }

    public List<String> getAlertPolicies() {
        return alertPolicies;
    }

    public void setAlertPolicies(List<String> alertPolicies) {
        this.alertPolicies = alertPolicies;
    }
}
