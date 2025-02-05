package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.service.AcceptanceOrRejectionService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@Import({MockMandateSentToPermissionAdminHandler.class, MockMandateSentToPermissionAdminHandlerTest.MockMandateSentToPermissionAdminHandlerTestContextConfiguration.class})
@TestPropertySource(properties = {"region-connector.be.fluvius.mock-mandates=true"})
class MockMandateSentToPermissionAdminHandlerTest {
    @MockitoBean
    private AcceptanceOrRejectionService acceptanceOrRejectionService;
    @Autowired
    private EventBus eventBus;

    @Test
    void testAccept_acceptsPermissionRequest() throws PermissionNotFoundException {
        // Given
        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));

        // Then
        verify(acceptanceOrRejectionService).acceptOrRejectPermissionRequest(
                assertArg(res -> assertEquals("pid", res)),
                eq(PermissionProcessStatus.ACCEPTED)
        );
    }


    @Test
    void testAccept_doesNothingOnUnknownPermissionRequest() throws PermissionNotFoundException {
        // Given
        doThrow(PermissionNotFoundException.class)
                .when(acceptanceOrRejectionService)
                .acceptOrRejectPermissionRequest("pid", PermissionProcessStatus.ACCEPTED);
        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));

        // Then
        verify(acceptanceOrRejectionService).acceptOrRejectPermissionRequest(any(), any());
    }

    @TestConfiguration
    static class MockMandateSentToPermissionAdminHandlerTestContextConfiguration {
        @Bean
        public EventBus eventBus() {
            return new EventBusImpl();
        }
    }
}

@ExtendWith(SpringExtension.class)
@Import({MockMandateSentToPermissionAdminHandler.class, MockMandateSentToPermissionAdminHandlerTest.MockMandateSentToPermissionAdminHandlerTestContextConfiguration.class})
@TestPropertySource(properties = {"region-connector.be.fluvius.mock-mandates=false"})
class MockMandateSentToPermissionAdminHandlerWithoutMockMandatesTest {
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testMockMandateSentToPermissionAdminHandler_doesNotExist() {
        // Given, When, Then
        assertThrows(NoSuchBeanDefinitionException.class,
                     () -> applicationContext.getBean(MockMandateSentToPermissionAdminHandler.class));
    }
}
