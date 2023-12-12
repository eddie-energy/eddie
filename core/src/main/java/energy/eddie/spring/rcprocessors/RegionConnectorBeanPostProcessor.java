package energy.eddie.spring.rcprocessors;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.spring.RegionConnectorProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;

import static java.util.Objects.requireNonNull;

@RegionConnectorProcessor
public class RegionConnectorBeanPostProcessor implements BeanPostProcessor {
    public static final String REGION_CONNECTOR_NAME_BEAN_NAME = "regionConnectorName";
    private final DefaultListableBeanFactory beanFactory;

    /**
     * This {@link BeanPostProcessor} extracts the value of {@link RegionConnector#name()}, creates a
     * String bean for the value, and registers this bean in the context of the region connector.
     * <p>
     * The bean will be registered with the name {@value REGION_CONNECTOR_NAME_BEAN_NAME}.
     * </p>
     *
     * @param beanFactory BeanFactory used by this context.
     */
    public RegionConnectorBeanPostProcessor(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = requireNonNull(beanFactory);
    }

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String unusedBeanName) {
        RegionConnector annotation = AnnotationUtils.findAnnotation(bean.getClass(), RegionConnector.class);
        if (annotation != null) {
            String connectorName = annotation.name();
            beanFactory.registerSingleton(REGION_CONNECTOR_NAME_BEAN_NAME, connectorName);
        }
        return bean;
    }
}
