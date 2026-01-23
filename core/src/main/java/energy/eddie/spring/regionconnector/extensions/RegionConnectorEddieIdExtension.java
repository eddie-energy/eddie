// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.ApplicationInformationAware;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.core.services.ApplicationInformationService;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;

import static java.util.Objects.requireNonNull;

@RegionConnectorExtension
public class RegionConnectorEddieIdExtension implements BeanPostProcessor {
    private final DefaultListableBeanFactory beanFactory;
    private final ApplicationInformationService applicationInformationService;

    /**
     * This {@link BeanPostProcessor} makes the bean aware of the EDDIE ID if it is annotated with {@link ApplicationInformationAware}.
     *
     * @param beanFactory BeanFactory used by this context.
     */
    public RegionConnectorEddieIdExtension(
            DefaultListableBeanFactory beanFactory,
            @Lazy ApplicationInformationService applicationInformationService
    ) {
        this.beanFactory = requireNonNull(beanFactory);
        this.applicationInformationService = requireNonNull(applicationInformationService);
    }

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String unusedBeanName) {
        ApplicationInformationAware annotation = AnnotationUtils.findAnnotation(bean.getClass(),
                                                                                ApplicationInformationAware.class);
        if (annotation != null) {
            var eddieId = applicationInformationService.applicationInformation().eddieId();
            beanFactory.registerSingleton(ApplicationInformationAware.BEAN_NAME, eddieId);
        }

        return bean;
    }
}
