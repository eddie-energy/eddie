// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.PingHealthIndicator;
import org.springframework.boot.health.registry.HealthContributorRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthIndicatorRegistrarTest {
    @Mock
    private ConfigurableListableBeanFactory beanFactory;
    @Mock
    private HealthContributorRegistry registry;
    @InjectMocks
    private HealthIndicatorRegistrar healthIndicatorRegistrar;

    @Test
    void registersHealthIndicators() {
        // Given
        HealthContributor bean = new PingHealthIndicator();
        when(beanFactory.getParentBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBean(HealthContributorRegistry.class)).thenReturn(registry);
        var iterator = List.of(new HealthContributors.Entry("first", bean)).iterator();
        when(registry.iterator()).thenReturn(iterator);

        // When
        healthIndicatorRegistrar.postProcessAfterInitialization(bean, "otherHealthIndicator");

        // Then
        verify(registry).registerContributor("otherHealthIndicator", bean);
    }

    @Test
    void registersHealthIndicator_withoutName() {
        // Given
        HealthContributor bean = new PingHealthIndicator();
        when(beanFactory.getParentBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBean(HealthContributorRegistry.class)).thenReturn(registry);

        // When
        healthIndicatorRegistrar.postProcessAfterInitialization(bean, null);

        // Then
        verify(registry).registerContributor("PingHealthIndicator", bean);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void doesNotRegisterNonHealthIndicators() {
        // Given
        when(beanFactory.getParentBeanFactory()).thenReturn(beanFactory);

        // When
        healthIndicatorRegistrar.postProcessAfterInitialization("Not a health indicator", "beanName");

        // Then
        verify(registry, never()).registerContributor(any(), any());
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void doesNotRegisterDuplicateHealthIndicators() {
        // Given
        var bean = new PingHealthIndicator();
        when(beanFactory.getParentBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBean(HealthContributorRegistry.class)).thenReturn(registry);
        var it = List.of(new HealthContributors.Entry("bean", bean))
                     .iterator();
        when(registry.iterator()).thenReturn(it);

        // When
        healthIndicatorRegistrar.postProcessAfterInitialization(bean, "beanHealthIndicator");

        // Then
        verify(registry, never()).registerContributor(any(), any());
    }

    @Test
    void registeringInNonChildContext_throws() {
        // Given
        var bean = new PingHealthIndicator();

        // When & Then
        assertThrows(NullPointerException.class,
                     () -> healthIndicatorRegistrar.postProcessAfterInitialization(bean, "otherHealthIndicator"));
    }
}