package energy.eddie.api.agnostic.process.model;

import java.time.LocalDate;

public interface TimeframedPermissionRequest extends PermissionRequest {
    /**
     * The start date from which data is requested. (inclusive)
     */
    LocalDate start();

    /**
     * The end date from which data is requested. (inclusive)
     */
    LocalDate end();
}
