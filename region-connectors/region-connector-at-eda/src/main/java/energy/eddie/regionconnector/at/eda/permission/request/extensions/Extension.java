package energy.eddie.regionconnector.at.eda.permission.request.extensions;

import energy.eddie.api.v0.process.model.PermissionRequest;

import java.util.function.Consumer;

@FunctionalInterface
public interface Extension<T extends PermissionRequest> extends Consumer<T> {
}
