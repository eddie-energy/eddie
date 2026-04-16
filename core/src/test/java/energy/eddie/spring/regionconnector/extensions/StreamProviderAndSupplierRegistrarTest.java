// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.core.services.MessageStreamHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamProviderAndSupplierRegistrarTest {
    @Mock
    private MessageStreamHub messageStreamHub;
    @Mock
    private ApplicationContext applicationContext;
    private StreamProviderAndSupplierRegistrar registrar;
    @Captor
    private ArgumentCaptor<Class<String>> typeCaptor;
    @Captor
    private ArgumentCaptor<Supplier<Flux<String>>> supplierCaptor;
    @Captor
    private ArgumentCaptor<Consumer<Flux<?>>> consumerCaptor;

    @BeforeEach
    void setUp() {
        registrar = new StreamProviderAndSupplierRegistrar();
        registrar.setApplicationContext(applicationContext);
        when(applicationContext.getBean(MessageStreamHub.class)).thenReturn(messageStreamHub);
    }

    @Test
    void testRegisterProviderWithMessageStreamAnnotation() {
        // Given
        Object provider = new Object() {
            @MessageStream(String.class)
            public Flux<String> provideMessages() {
                return Flux.just("test1", "test2");
            }
        };
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"testProvider"});
        when(applicationContext.getBean("testProvider")).thenReturn(provider);

        // When
        registrar.afterSingletonsInstantiated();

        // Then
        verify(messageStreamHub).registerProvider(typeCaptor.capture(), supplierCaptor.capture());

        assertEquals(String.class, typeCaptor.getValue());
        assertNotNull(supplierCaptor.getValue().get());
    }

    @Test
    void testRegisterConsumerWithMessageStreamAnnotation() {
        // Given
        Object consumer = new Object() {
            @MessageStream(String.class)
            public void accept(Flux<String> messages) {
                // No Op
            }
        };
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"testConsumer"});
        when(applicationContext.getBean("testConsumer")).thenReturn(consumer);

        // When
        registrar.afterSingletonsInstantiated();

        // Then
        verify(messageStreamHub).registerReceiver(typeCaptor.capture(), consumerCaptor.capture());

        assertEquals(String.class, typeCaptor.getValue());
    }

    @Test
    void testRegisterWithoutAnyAnnotatedMethodDoesNothing() {
        // Given
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"bean"});
        when(applicationContext.getBean("bean")).thenReturn(new Object());

        // When
        registrar.afterSingletonsInstantiated();

        // Then
        verifyNoInteractions(messageStreamHub);
    }

    @ParameterizedTest
    @MethodSource("testRegisterWithoutCorrectMethodSignature")
    void testRegisterWithoutCorrectMethodSignature(Object bean) {
        // Given
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"bean"});
        when(applicationContext.getBean("bean")).thenReturn(bean);

        // When & Then
        assertThrows(IllegalStateException.class, () -> registrar.afterSingletonsInstantiated());
        verifyNoInteractions(messageStreamHub);
    }

    private static Stream<Arguments> testRegisterWithoutCorrectMethodSignature() {
        return Stream.of(
                Arguments.of(new Supplier<String>() {
                    @MessageStream(String.class)
                    public String get() {
                        return "test";
                    }
                }),
                Arguments.of(new Object() {
                    @MessageStream(Integer.class)
                    public Flux<String> someMethod(Flux<String> ignored) {
                        return Flux.just("test");
                    }
                }),
                Arguments.of(new Consumer<String>() {
                    @MessageStream(String.class)
                    public void accept(String ignored) {
                        // No-Op
                    }
                })
        );
    }
}