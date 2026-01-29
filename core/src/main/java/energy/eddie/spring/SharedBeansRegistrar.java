// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring;


import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.utils.Shared;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * {@link BeanPostProcessor} that checks for all beans whether it (or any ancestor interfaces of the Bean) is annotated
 * with {@link Shared}. It will re-register any annotated Bean in the parent context.
 * <br>
 * <br>
 * To add support for any region connector to share their Beans with the parent context, annotate this class with
 * {@link RegionConnectorExtension} and move it into the same package as the other extensions.
 */
public class SharedBeansRegistrar implements BeanPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedBeansRegistrar.class);
    private final ConfigurableListableBeanFactory localBeanFactory;

    /**
     * @see SharedBeansRegistrar
     */
    public SharedBeansRegistrar(ConfigurableListableBeanFactory localBeanFactory) {
        this.localBeanFactory = localBeanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(
            @NonNull Object bean,
            @NonNull String beanName
    ) throws BeansException {
        ConfigurableListableBeanFactory parentBeanFactory = getParentBeanFactory();
        Objects.requireNonNull(parentBeanFactory, "This post processor can only be called in a child context");

        BeanDefinition beanDefinition;
        try {
            beanDefinition = localBeanFactory.getBeanDefinition(beanName);
        } catch (NoSuchBeanDefinitionException exception) {
            return bean;
        }

        if (shouldBeanBeShared(beanDefinition)) {
            LOGGER.info("Sharing bean '{}' with parent context.", beanName);
            parentBeanFactory.registerSingleton(beanName, bean);
        }

        return bean;
    }

    /**
     * Checks whether the class or any of its ancestor interfaces is annotated with {@link Shared}.
     *
     * @param beanDefinition BeanDefinition to check
     * @return True if the class of {@code beanDefinition} or any of its ancestor interfaces is annotated with
     * {@link Shared}.
     */
    private boolean shouldBeanBeShared(BeanDefinition beanDefinition) {
        // class itself is annotated
        if (beanDefinition.getSource() instanceof StandardMethodMetadata metadata && metadata.isAnnotated(Shared.class.getName()))
            return true;

        Class<?> clazz;
        try {
            if (beanDefinition.getBeanClassName() == null)
                return false;

            clazz = Class.forName(beanDefinition.getBeanClassName());
            return Arrays.stream(clazz.getInterfaces()).anyMatch(iface -> iface.isAnnotationPresent(Shared.class));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns the parent bean factory of the current context or null if none is available.
     *
     * @return Parent bean factory or null if not available.
     */
    @Nullable
    private ConfigurableListableBeanFactory getParentBeanFactory() {
        BeanFactory parent = localBeanFactory.getParentBeanFactory();
        return (parent instanceof ConfigurableListableBeanFactory factory) ? factory : null;
    }
}
