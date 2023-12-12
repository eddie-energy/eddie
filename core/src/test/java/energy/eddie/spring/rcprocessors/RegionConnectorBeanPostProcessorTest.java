package energy.eddie.spring.rcprocessors;

import energy.eddie.api.agnostic.RegionConnector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static energy.eddie.spring.rcprocessors.RegionConnectorBeanPostProcessor.REGION_CONNECTOR_NAME_BEAN_NAME;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RegionConnectorBeanPostProcessorTest {
    @Test
    void givenNull_constructor_throws() {
        // Given, When, Then
        assertThrows(NullPointerException.class, () -> new RegionConnectorBeanPostProcessor(null));
    }

    @Test
    void givenBeanWithoutAnnotation_doesNothing() {
        // Given
        var mockFactory = mock(DefaultListableBeanFactory.class);
        var processor = new RegionConnectorBeanPostProcessor(mockFactory);
        var bean = new NotAnnotatedClass();

        // When
        processor.postProcessBeforeInitialization(bean, "bla");

        // Then
        verifyNoInteractions(mockFactory);
    }

    @Test
    void givenBeanWithAnnotation_extractsName_andCreatesBeanForName() {
        // Given
        var mockFactory = mock(DefaultListableBeanFactory.class);
        var processor = new RegionConnectorBeanPostProcessor(mockFactory);
        var bean = new AnnotatedClass();

        // When
        processor.postProcessBeforeInitialization(bean, "foo");

        // Then
        verify(mockFactory).registerSingleton(REGION_CONNECTOR_NAME_BEAN_NAME, "TestName");
    }

    private static class NotAnnotatedClass {
    }

    @RegionConnector(name = "TestName")
    private static class AnnotatedClass {
    }
}
