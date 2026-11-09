// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.core.env.MapPropertySource;
import reactor.core.publisher.Flux;

import java.util.Map;

import static energy.eddie.regionconnector.us.green.button.security.WebhookSecurityConfig.US_GREEN_BUTTON_ENABLED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataProviderTest {
    @Mock
    private PublishService publishService;
    @Mock
    private Jaxb2Marshaller marshaller;
    @Mock
    private CommonInformationModelConfiguration config;
    @Mock
    private GreenButtonConfiguration greenButtonConfig;

    @Test
    void wiresDistinctVersionedBeansWithConcreteMessageStreamDeclarations() throws Exception {
        when(publishService.validatedHistoricalData()).thenReturn(Flux.never());
        try (var context = new AnnotationConfigApplicationContext()) {
            context.setAllowBeanDefinitionOverriding(false);
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("connector", Map.of(US_GREEN_BUTTON_ENABLED, "true"))
            );
            context.registerBean("publishService", PublishService.class, () -> publishService);
            context.registerBean("marshaller", Jaxb2Marshaller.class, () -> marshaller);
            context.registerBean("config", CommonInformationModelConfiguration.class, () -> config);
            context.registerBean("greenButtonConfig", GreenButtonConfiguration.class, () -> greenButtonConfig);
            context.register(energy.eddie.regionconnector.us.green.button.providers.v1_04.ValidatedHistoricalDataProvider.class,
                             ValidatedHistoricalDataProvider.class);
            context.refresh();

            var sourceType = energy.eddie.regionconnector.us.green.button.providers.v1_04.ValidatedHistoricalDataProvider.class;
            var source = context.getBean(sourceType);
            var adapter = context.getBean(ValidatedHistoricalDataProvider.class);
            assertSame(source, context.getBean("v104ValidatedHistoricalDataProvider"));
            assertSame(adapter, context.getBean("greenButtonValidatedHistoricalDataProviderV112"));
            assertEquals(1, context.getBeanNamesForType(sourceType).length);
            assertEquals(1, context.getBeanNamesForType(ValidatedHistoricalDataProvider.class).length);
            assertEquals(VHDEnvelope.class, sourceType.getDeclaredMethod("getValidatedHistoricalDataMarketDocumentsStream")
                                                     .getAnnotation(MessageStream.class).value());
            assertThrows(NoSuchMethodException.class, () -> sourceType.getDeclaredMethod("getValidatedHistoricalDataV112Stream"));
            assertEquals(Envelope.class, ValidatedHistoricalDataProvider.class.getDeclaredMethod("getValidatedHistoricalDataV112Stream")
                                                                              .getAnnotation(MessageStream.class).value());
            assertNotNull(adapter.getValidatedHistoricalDataV112Stream());
        }
    }
}
