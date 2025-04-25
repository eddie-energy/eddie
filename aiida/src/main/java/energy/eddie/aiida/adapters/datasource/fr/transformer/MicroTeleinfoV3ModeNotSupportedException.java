package energy.eddie.aiida.adapters.datasource.fr.transformer;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MicroTeleinfoV3ModeNotSupportedException extends Exception {
    private static final String MESSAGE = "Mode %s not applicable for the given payload.";
    private final String payload;

    public MicroTeleinfoV3ModeNotSupportedException(byte[] payload, List<MicroTeleinfoV3Mode> mode) {
        super(MESSAGE.formatted(mode));
        this.payload = new String(payload, StandardCharsets.UTF_8);
    }

    public String payload() {
        return payload;
    }
}
