// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RegionConnectorNameExtensionTest {
    @Test
    void givenNull_constructor_throws() {
        // Given, When, Then
        assertThrows(NullPointerException.class, () -> new RegionConnectorNameExtension(null));
    }

    @Test
    void givenBeanWithoutAnnotation_doesNothing() {
        // Given
        var mockFactory = mock(DefaultListableBeanFactory.class);
        var processor = new RegionConnectorNameExtension(mockFactory);
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
        var processor = new RegionConnectorNameExtension(mockFactory);
        var bean = new AnnotatedClass();

        // When
        processor.postProcessBeforeInitialization(bean, "foo");

        // Then
        verify(mockFactory).registerSingleton(RegionConnectorNameExtension.REGION_CONNECTOR_NAME_BEAN_NAME, "TestName");
    }

    private static class NotAnnotatedClass {
    }

    @RegionConnector(name = "TestName")
    private static class AnnotatedClass {
    }
}
