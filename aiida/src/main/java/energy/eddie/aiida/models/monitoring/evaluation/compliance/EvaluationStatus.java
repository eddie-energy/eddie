package energy.eddie.aiida.models.monitoring.evaluation.compliance;

public enum EvaluationStatus {
    VIOLATION("violation"),
    NO_DATA("no_data"),
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
