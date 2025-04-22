package energy.eddie.aiida.models.datasource.mqtt;

import java.security.SecureRandom;

public class MqttSecretGenerator {
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int LENGTH = 10;

    private MqttSecretGenerator() {
        throw new IllegalStateException("Utility class");
    }

    public static String generate() {
        StringBuilder builder = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(BASE62.length());
            builder.append(BASE62.charAt(index));
        }
        return builder.toString();
    }
}
