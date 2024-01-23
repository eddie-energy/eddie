package energy.eddie.regionconnector.shared.permission.requests;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.annotations.InvokeExtensions;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SimplePermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PermissionRequestProxyTest {
    @Test
    @SuppressWarnings("unchecked")
    void testPermissionRequestProxyInvocation() throws Throwable {
        // Given
        PermissionRequest delegate = mock(PermissionRequest.class);
        Set<Extension<PermissionRequest>> consumers = new HashSet<>();
        Extension<PermissionRequest> consumer1 = mock(Extension.class);
        Extension<PermissionRequest> consumer2 = mock(Extension.class);
        consumers.add(consumer1);
        consumers.add(consumer2);

        PermissionRequestProxy<PermissionRequest> proxy = new PermissionRequestProxy<>(
                delegate, consumers, PermissionRequestProxy.CreationInfo.RECREATED);

        // When
        Method method = PermissionRequest.class.getDeclaredMethod("accept");
        proxy.invoke(proxy, method, null);

        // Then
        verify(delegate).accept();
        verify(consumer1).accept(delegate);
        verify(consumer2).accept(delegate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateProxy() {
        // Given
        PermissionRequest delegate = new SimplePermissionRequest("pid", "cid", null, "dnid");
        Set<Extension<PermissionRequest>> consumers = new HashSet<>();
        Extension<PermissionRequest> consumer1 = mock(Extension.class);
        Extension<PermissionRequest> consumer2 = mock(Extension.class);
        consumers.add(consumer1);
        consumers.add(consumer2);

        // When
        PermissionRequest proxy = PermissionRequestProxy.createProxy(
                delegate, consumers, PermissionRequest.class, PermissionRequestProxy.CreationInfo.NEWLY_CREATED);

        // Then
        assertEquals("pid", proxy.permissionId());
        assertEquals("cid", proxy.connectionId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPermissionRequestProxyInvocationOfInvokeExtensionAnnotation() throws Throwable {
        // Given
        SimplePermissionRequestExtension delegate = mock(SimplePermissionRequestExtension.class);
        Set<Extension<SimplePermissionRequestExtension>> consumers = new HashSet<>();
        Extension<SimplePermissionRequestExtension> consumer1 = mock(Extension.class);
        Extension<SimplePermissionRequestExtension> consumer2 = mock(Extension.class);
        consumers.add(consumer1);
        consumers.add(consumer2);

        var proxy = PermissionRequestProxy.createProxy(
                delegate,
                consumers,
                SimplePermissionRequestExtension.class,
                PermissionRequestProxy.CreationInfo.RECREATED
        );

        // When
        proxy.testMethod(1, "2");

        // Then
        verify(delegate).testMethod(1, "2");
        verify(consumer1).accept(delegate);
        verify(consumer2).accept(delegate);
    }

    interface SimplePermissionRequestExtension extends PermissionRequest {
        @InvokeExtensions
        String testMethod(int x, String y);
    }
}