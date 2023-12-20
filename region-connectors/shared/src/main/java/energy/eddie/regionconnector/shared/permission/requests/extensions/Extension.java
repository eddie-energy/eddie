package energy.eddie.regionconnector.shared.permission.requests.extensions;

import energy.eddie.api.v0.process.model.PermissionRequest;

import java.util.function.Consumer;

@FunctionalInterface
public interface Extension<T extends PermissionRequest> extends Consumer<T> {
}
