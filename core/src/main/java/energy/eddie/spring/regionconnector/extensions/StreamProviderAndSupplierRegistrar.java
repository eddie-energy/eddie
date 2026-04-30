// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.outbound.OutboundConnectorExtension;
import energy.eddie.cim.agnostic.RawDataMessage;
import energy.eddie.core.services.MessageStreamHub;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;

import static energy.eddie.core.message.streams.MessageStreamUtils.*;

/**
 * Registrar for stream providers and suppliers within a Spring application context.
 * This class scans the Spring context for beans with methods annotated with
 * {@link MessageStream}. Based on the annotated methods, it registers providers
 * and receivers into a {@link MessageStreamHub}.
 */
@OutboundConnectorExtension
@RegionConnectorExtension
public class StreamProviderAndSupplierRegistrar implements SmartInitializingSingleton, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamProviderAndSupplierRegistrar.class);
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        LOGGER.debug("Registering Stream Providers and Suppliers in spring context {}", applicationContext.getId());
        var hub = applicationContext.getBean(MessageStreamHub.class);
        var environment = applicationContext.getEnvironment();

        for (var beanName : applicationContext.getBeanDefinitionNames()) {
            var bean = applicationContext.getBean(beanName);
            var targetClass = AopUtils.getTargetClass(bean);

            for (var method : targetClass.getDeclaredMethods()) {
                var annotation = method.getAnnotation(MessageStream.class);
                if (annotation == null || !method.trySetAccessible()) {
                    continue;
                }

                var messageType = annotation.value();

                if (messageType.equals(RawDataMessage.class) && !areRawDataMessagesEnabled(environment)) {
                    LOGGER.debug(
                            "Skipping raw data message stream {}#{} because eddie.raw.data.output.enabled=false",
                            targetClass.getName(),
                            method.getName()
                    );
                } else if (isProviderMethod(method)) {
                    LOGGER.info("Registering provider {}#{} for message type {} in spring context {}",
                                targetClass.getName(),
                                method.getName(),
                                messageType.getName(),
                                applicationContext.getId());
                    registerProvider(hub, messageType, bean, method);
                } else if (isReceiverMethod(method)) {
                    LOGGER.info("Registering receiver {}#{} for message type {} in spring context {}",
                                targetClass.getName(),
                                method.getName(),
                                messageType.getName(),
                                applicationContext.getId());
                    registerReceiver(hub, messageType, bean, method);
                } else {
                    throw new IllegalStateException(
                            "@MessageStream method must either return Flux<T> or accept exactly one Flux<T> parameter: "
                            + targetClass.getName() + "#" + method.getName()
                    );
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerProvider(
            MessageStreamHub hub,
            Class<?> messageType,
            Object bean,
            Method method
    ) {
        hub.registerProvider((Class) messageType, () -> {
            try {
                return (Flux<?>) method.invoke(bean);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to invoke provider method: " + method, e);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerReceiver(
            MessageStreamHub hub,
            Class<?> messageType,
            Object bean,
            Method method
    ) {
        hub.registerReceiver((Class) messageType, messages -> {
            try {
                method.invoke(bean, messages);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to invoke receiver method: " + method, e);
            }
        });
    }
}