package energy.eddie.aiida.models.monitoring.openslo.alert;

@SuppressWarnings("NullAway")
public class AlertPolicyConditionValue {
    private String kind;
    private String op;
    private double threshold;
    private String lookbackWindow;
    private String alertAfter;

    public AlertPolicyConditionValue() {

    }

    public AlertPolicyConditionValue(String kind, String op, double threshold, String lookbackWindow, String alertAfter) {
        this.kind = kind;
        this.op = op;
        this.threshold = threshold;
        this.lookbackWindow = lookbackWindow;
        this.alertAfter = alertAfter;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getLookbackWindow() {
        return lookbackWindow;
    }

    public void setLookbackWindow(String lookbackWindow) {
        this.lookbackWindow = lookbackWindow;
    }

    public String getAlertAfter() {
        return alertAfter;
    }

    public void setAlertAfter(String alertAfter) {
        this.alertAfter = alertAfter;
    }
}
