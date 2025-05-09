package energy.eddie.aiida.models.monitoring.openslo.slo;

@SuppressWarnings("NullAway")
public class SloObjective {
    private String op;
    private double target;

    public SloObjective() {
    }

    public SloObjective(String op, double target) {
        this.op = op;
        this.target = target;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public double getTarget() {
        return target;
    }

    public void setTarget(double target) {
        this.target = target;
    }
}
