package energy.eddie.aiida.models.monitoring.evaluation.compliance;

public enum EvaluationStatus {
    VIOLATION("violation"),
    NO_METRICS("no_metrics"),
    COMPLIANT("compliant"),
    PENDING("pending");

    private final String status;

    EvaluationStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
