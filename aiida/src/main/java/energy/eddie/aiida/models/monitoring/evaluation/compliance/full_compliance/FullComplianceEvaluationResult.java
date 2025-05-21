package energy.eddie.aiida.models.monitoring.evaluation.compliance.full_compliance;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.EvaluationPeriod;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.EvaluationResult;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.EvaluationStatus;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.RateEvaluationResult;

public class FullComplianceEvaluationResult extends EvaluationResult {
    private final RateEvaluationResult rateEvaluationResult;

    public FullComplianceEvaluationResult(String sloId, EvaluationStatus status, EvaluationPeriod evaluationPeriod, RateEvaluationResult rateEvaluationResult) {
        super(sloId, status, evaluationPeriod);
        this.rateEvaluationResult = rateEvaluationResult;
    }

    @JsonProperty("complianceRate")
    public RateEvaluationResult getBurnRateEvaluationResult() {
        return rateEvaluationResult;
    }
}
