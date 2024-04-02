package energy.eddie.regionconnector.at.eda.ponton.messages;

import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke.CMRevokeOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import(MarshallerConfig.class)
class OutboundMessageFactoryCollectionTest {

    @Autowired
    protected Jaxb2Marshaller marshaller;

    public static Stream<Arguments> noActiveFactoriesCtrArguments() {
        return Stream.of(
                Arguments.of(List.of(), List.of()),
                Arguments.of(List.of(simpleCMRequestFactory(false)), List.of(simpleCMRevokeFactory(true))),
                Arguments.of(List.of(simpleCMRequestFactory(true)), List.of(simpleCMRevokeFactory(false))),
                Arguments.of(List.of(simpleCMRequestFactory(false)), List.of(simpleCMRevokeFactory(false)))
        );
    }

    private static CMRequestOutboundMessageFactory simpleCMRequestFactory(boolean isActive) {
        return new CMRequestOutboundMessageFactory() {
            @Override
            public OutboundMessage createOutboundMessage(CCMORequest ccmoRequest) {
                return null;
            }

            @Override
            public boolean isActive(LocalDate date) {
                return isActive;
            }
        };
    }

    private static CMRevokeOutboundMessageFactory simpleCMRevokeFactory(boolean isActive) {
        return new CMRevokeOutboundMessageFactory() {
            @Override
            public OutboundMessage createOutboundMessage(CCMORevoke ccmoRevoke) {
                return null;
            }

            @Override
            public boolean isActive(LocalDate date) {
                return isActive;
            }
        };
    }

    @ParameterizedTest
    @MethodSource("noActiveFactoriesCtrArguments")
    void ctr_with_noActiveFactoriesThrows(
            List<CMRequestOutboundMessageFactory> outboundCMRequestFactories,
            List<CMRevokeOutboundMessageFactory> outboundCMRevokeFactories
    ) {
        assertThrows(IllegalStateException.class,
                     () -> new OutboundMessageFactoryCollection(outboundCMRequestFactories, outboundCMRevokeFactories));
    }

    @Test
    void updateActiveFactory_updatesRevokeFactory() {
        // Given
        CMRevokeOutboundMessageFactory revokeFactory1 = mock(CMRevokeOutboundMessageFactory.class);
        when(revokeFactory1.isActive(any())).thenReturn(true).thenReturn(false);

        CMRevokeOutboundMessageFactory revokeFactory2 = mock(CMRevokeOutboundMessageFactory.class);
        when(revokeFactory2.isActive(any())).thenReturn(true);

        var collection = new OutboundMessageFactoryCollection(List.of(simpleCMRequestFactory(true)),
                                                              List.of(revokeFactory1, revokeFactory2));

        assertEquals(revokeFactory1, collection.activeCmRevokeFactory());

        // When
        collection.updateActiveFactories();

        // Then
        assertEquals(revokeFactory2, collection.activeCmRevokeFactory());
    }

    @Test
    void updateActiveFactory_updatesRequestFactory() {
        // Given
        CMRequestOutboundMessageFactory requestFactory = mock(CMRequestOutboundMessageFactory.class);
        when(requestFactory.isActive(any())).thenReturn(true).thenReturn(false);
        CMRevokeOutboundMessageFactory revokeFactory = mock(CMRevokeOutboundMessageFactory.class);
        when(revokeFactory.isActive(any())).thenReturn(true).thenReturn(false);


        var collection = new OutboundMessageFactoryCollection(List.of(requestFactory),
                                                              List.of(revokeFactory));

        assertAll(
                () -> assertEquals(requestFactory, collection.activeCmRequestFactory()),
                () -> assertEquals(revokeFactory, collection.activeCmRevokeFactory())
        );

        // When
        collection.updateActiveFactories();

        // Then
        assertAll(
                () -> assertEquals(requestFactory, collection.activeCmRequestFactory()),
                () -> assertEquals(revokeFactory, collection.activeCmRevokeFactory())
        );
    }

    @Test
    void updateActiveFactory_doesNotUpdateFactoriesIfNoActiveFactoryFound() {
        // Given
        CMRequestOutboundMessageFactory requestFactory1 = mock(CMRequestOutboundMessageFactory.class);
        when(requestFactory1.isActive(any())).thenReturn(true).thenReturn(false);

        CMRequestOutboundMessageFactory requestFactory2 = mock(CMRequestOutboundMessageFactory.class);
        when(requestFactory2.isActive(any())).thenReturn(true);

        var collection = new OutboundMessageFactoryCollection(List.of(requestFactory1, requestFactory2),
                                                              List.of(simpleCMRevokeFactory(true)));

        assertEquals(requestFactory1, collection.activeCmRequestFactory());

        // When
        collection.updateActiveFactories();

        // Then
        assertEquals(requestFactory2, collection.activeCmRequestFactory());
    }


    @Test
    void activeCmRequestFactory() {
        CMRequestOutboundMessageFactory factory = simpleCMRequestFactory(true);
        var collection = new OutboundMessageFactoryCollection(List.of(factory),
                                                              List.of(simpleCMRevokeFactory(true)));

        assertEquals(factory, collection.activeCmRequestFactory());
    }

    @Test
    void activeCmRevokeFactory() {
        CMRevokeOutboundMessageFactory factory = simpleCMRevokeFactory(true);
        var collection = new OutboundMessageFactoryCollection(List.of(simpleCMRequestFactory(true)),
                                                              List.of(factory));

        assertEquals(factory, collection.activeCmRevokeFactory());
    }
}
