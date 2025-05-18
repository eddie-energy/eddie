package energy.eddie.aiida.models.monitoring.evaluation.compliance;

public enum EvaluationType {
    NEAR_REALTIME("burnRate"),
    COMPLIANCE("compliance");

    private final String type;

    EvaluationType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
