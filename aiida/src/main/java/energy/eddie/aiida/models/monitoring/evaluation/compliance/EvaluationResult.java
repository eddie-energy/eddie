package energy.eddie.aiida.models.monitoring.evaluation.compliance;

public abstract class EvaluationResult {
    private final String sloId;
    private final EvaluationStatus status;
    private final EvaluationPeriod evaluationPeriod;

    public EvaluationResult(String sloId, EvaluationStatus status, EvaluationPeriod evaluationPeriod) {
        this.sloId = sloId;
        this.status = status;
        this.evaluationPeriod = evaluationPeriod;
    }

    public String getSloId() {
        return sloId;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public EvaluationPeriod getEvaluationPeriod() {
        return evaluationPeriod;
    }
}
