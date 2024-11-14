package energy.eddie.api.agnostic;

import energy.eddie.api.agnostic.process.model.PermissionRequest;

/**
 * Interface for all the classes that have a permission request and a payload, such as IdentifiableMeterReading.
 *
 * @param <P> The type of the permission request
 * @param <R> The type of the payload, usually just the format that is provided by the metering data administrator or permission administrator
 */
public interface IdentifiablePayload<P extends PermissionRequest, R> {
    P permissionRequest();

    R payload();
}
