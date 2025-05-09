package energy.eddie.aiida.models.monitoring.openslo.slo;

@SuppressWarnings("NullAway")
public class SloTimeWindow {
    private String duration;
    private boolean isRolling;

    public SloTimeWindow() {

    }

    public SloTimeWindow(String duration, boolean isRolling) {
        this.duration = duration;
        this.isRolling = isRolling;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean isRolling() {
        return isRolling;
    }

    public void setIsRolling(boolean isRolling) {
        this.isRolling = isRolling;
    }
}
