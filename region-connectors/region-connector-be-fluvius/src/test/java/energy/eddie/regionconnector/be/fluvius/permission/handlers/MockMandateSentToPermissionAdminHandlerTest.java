package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@Import({MockMandateSentToPermissionAdminHandler.class, MockMandateSentToPermissionAdminHandlerTest.MockMandateSentToPermissionAdminHandlerTestContextConfiguration.class})
@TestPropertySource(properties = {"region-connector.be.fluvius.mock-mandates=true"})
class MockMandateSentToPermissionAdminHandlerTest {
    @MockBean
    private Outbox outbox;
    @Autowired
    private EventBus eventBus;

    @Test
    void testAccept_emitsAcceptedEvent() {
        // Given
        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));

        // Then
        verify(outbox).commit(assertArg(res -> assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, res.status())
        )));
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
