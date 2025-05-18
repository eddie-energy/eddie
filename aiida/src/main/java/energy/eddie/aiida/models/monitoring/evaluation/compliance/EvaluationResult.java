package energy.eddie.aiida.models.monitoring.evaluation.compliance;

public abstract class EvaluationResult {
    private final String timestamp;
    private final String service;
    private final String sloId;
    private final EvaluationType type;
    private final EvaluationStatus status;

    public EvaluationResult(String timestamp, String service, String sloId, EvaluationType type, EvaluationStatus status) {
        this.timestamp = timestamp;
        this.service = service;
        this.sloId = sloId;
        this.type = type;
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getService() {
        return service;
    }

    public String getSloId() {
        return sloId;
    }

    public EvaluationType getType() {
        return type;
    }

    public EvaluationStatus getStatus() {
        return status;
    }
}
