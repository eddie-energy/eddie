package energy.eddie.aiida.models.monitoring.evaluation.compliance;

import java.util.List;

public record ServiceEvaluationResult(String timestamp, String service, EvaluationType type, ServiceStatus status,
                                      List<EvaluationResult> results) {
}