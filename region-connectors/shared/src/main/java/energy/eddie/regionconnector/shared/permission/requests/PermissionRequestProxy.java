package energy.eddie.regionconnector.shared.permission.requests;

import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

public class PermissionRequestProxy<T extends PermissionRequest> implements InvocationHandler {
    private final T delegate;
    private final Set<Extension<T>> consumers;

    public PermissionRequestProxy(T delegate, Set<Extension<T>> consumers, CreationInfo creationInfo) {
        this.delegate = delegate;
        this.consumers = consumers;
        if (creationInfo == CreationInfo.NEWLY_CREATED) {
            executeConsumers();
        }
    }

    /**
     * This method will create a proxy instance of a permission request.
     * The proxy adds extra functionality via extensions.
     * It is unchecked for because of the reflection.
     *
     * @param delegate     the permission request that should be proxied
     * @param extensions   the extra functionality to extend the delegate with
     * @param clazz        the base class that the proxy should use
     * @param creationInfo if the proxy should run the extensions upon creation or only when the proxied methods are called
     * @param <T>          a type that is based on the permission request interface
     * @return a proxy object that adds functionality
     */
    @SuppressWarnings("unchecked")
    public static <T extends PermissionRequest> T createProxy(T delegate, Set<Extension<T>> extensions, Class<T> clazz, CreationInfo creationInfo) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[]{clazz},
                new PermissionRequestProxy<>(delegate, extensions, creationInfo)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(delegate, args);
        executeConsumers(method);
        return result;
    }

    private void executeConsumers(Method method) {
        if (Void.TYPE.equals(method.getReturnType()) && method.getParameterCount() == 0) {
            executeConsumers();
        }
    }

    private void executeConsumers() {
        consumers.forEach(consumer -> consumer.accept(delegate));
    }

    public enum CreationInfo {
        NEWLY_CREATED,
        RECREATED
    }
}
