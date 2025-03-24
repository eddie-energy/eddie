package energy.eddie.aiida.errors;

public class MqttUnauthorizedException extends Exception {
    public MqttUnauthorizedException(String message) {
        super(message);
    }
}
