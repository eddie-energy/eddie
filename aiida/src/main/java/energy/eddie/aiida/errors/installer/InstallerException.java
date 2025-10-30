package energy.eddie.aiida.errors.installer;

import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;

public class InstallerException extends Exception {
    private final HttpStatus httpStatus;

    public InstallerException(HttpStatus httpStatus, @Nullable String errorMessage) {
        super(errorMessage);
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
