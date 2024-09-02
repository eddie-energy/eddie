package energy.eddie.api.agnostic;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

/**
 * Interface for all the classes that have a permission request and a payload, such as IdentifiableMeterReading.
 */
public interface IdentifiablePayload<P extends PermissionRequest, R> {
    P permissionRequest();

    R payload();
}
