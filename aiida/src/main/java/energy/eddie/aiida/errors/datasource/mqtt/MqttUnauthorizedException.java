package energy.eddie.aiida.errors.datasource.mqtt;

public class MqttUnauthorizedException extends Exception {
    public MqttUnauthorizedException(String message) {
        super(message);
    }
}
