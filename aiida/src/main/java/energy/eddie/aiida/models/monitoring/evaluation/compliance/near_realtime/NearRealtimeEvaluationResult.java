package energy.eddie.aiida.models.monitoring.evaluation.compliance.near_realtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.EvaluationResult;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.EvaluationStatus;
import energy.eddie.aiida.models.monitoring.evaluation.compliance.EvaluationType;

public class NearRealtimeEvaluationResult extends EvaluationResult {
    private final BurnRateEvaluationResult burnRateEvaluationResult;

    public NearRealtimeEvaluationResult(String timestamp, String service, String sloId, EvaluationType type, EvaluationStatus status, BurnRateEvaluationResult burnRateEvaluationResult) {
        super(timestamp, service, sloId, type, status);
        this.burnRateEvaluationResult = burnRateEvaluationResult;
    }

    @JsonProperty("burnRate")
    public BurnRateEvaluationResult getBurnRateEvaluationResult() {
        return burnRateEvaluationResult;
    }
}
