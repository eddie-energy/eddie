package energy.eddie.regionconnector.at.eda.ponton.messages;

import java.time.LocalDate;

public interface PontonMessageFactory {
    boolean isActive(LocalDate date);
}
