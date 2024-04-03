package energy.eddie.regionconnector.at.eda.ponton.messages;

import energy.eddie.regionconnector.at.eda.dto.EdaCMNotification;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification.EdaCMNotificationInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke.EdaCMRevokeInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord.EdaConsumptionRecordInboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata.EdaMasterDataInboundMessageFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InboundMessageFactoryCollectionTest {

    public static Stream<Arguments> noActiveFactoriesCtrArguments() {
        return Stream.of(
                Arguments.of(List.of(), List.of(), List.of(), List.of()),
                Arguments.of(List.of(simpleEdaConsumptionRecordFactory(false)),
                             List.of(simpleEdaMasterFactory(true)),
                             List.of(simpleEdaCMNotificationFactory(true)),
                             List.of(simpleEdaCMRevokeFactory(true))),
                Arguments.of(List.of(simpleEdaConsumptionRecordFactory(true)),
                             List.of(simpleEdaMasterFactory(false)),
                             List.of(simpleEdaCMNotificationFactory(true)),
                             List.of(simpleEdaCMRevokeFactory(true))),
                Arguments.of(List.of(simpleEdaConsumptionRecordFactory(true)),
                             List.of(simpleEdaMasterFactory(true)),
                             List.of(simpleEdaCMNotificationFactory(false)),
                             List.of(simpleEdaCMRevokeFactory(true))),
                Arguments.of(List.of(simpleEdaConsumptionRecordFactory(true)),
                             List.of(simpleEdaMasterFactory(true)),
                             List.of(simpleEdaCMNotificationFactory(true)),
                             List.of(simpleEdaCMRevokeFactory(false)))
        );
    }

    private static EdaConsumptionRecordInboundMessageFactory simpleEdaConsumptionRecordFactory(boolean isActive) {
        return new EdaConsumptionRecordInboundMessageFactory() {
            @Override
            public EdaConsumptionRecord parseInputStream(InputStream inputStream) {
                return null;
            }

            @Override
            public boolean isActive(LocalDate date) {
                return isActive;
            }
        };
    }

    private static EdaMasterDataInboundMessageFactory simpleEdaMasterFactory(boolean isActive) {
        return new EdaMasterDataInboundMessageFactory() {
            @Override
            public EdaMasterData parseInputStream(InputStream inputStream) {
                return null;
            }

            @Override
            public boolean isActive(LocalDate date) {
                return isActive;
            }
        };
    }

    private static EdaCMNotificationInboundMessageFactory simpleEdaCMNotificationFactory(boolean isActive) {
        return new EdaCMNotificationInboundMessageFactory() {
            @Override
            public EdaCMNotification parseInputStream(InputStream inputStream) {
                return null;
            }

            @Override
            public boolean isActive(LocalDate date) {
                return isActive;
            }
        };
    }

    private static EdaCMRevokeInboundMessageFactory simpleEdaCMRevokeFactory(boolean isActive) {
        return new EdaCMRevokeInboundMessageFactory() {
            @Override
            public EdaCMRevoke parseInputStream(InputStream inputStream) {
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
            List<EdaConsumptionRecordInboundMessageFactory> inboundConsumptionRecordFactories,
            List<EdaMasterDataInboundMessageFactory> inboundMasterDataFactories,
            List<EdaCMNotificationInboundMessageFactory> inboundCMNotificationFactories,
            List<EdaCMRevokeInboundMessageFactory> inboundCMRevokeFactories
    ) {
        assertThrows(IllegalStateException.class,
                     () -> new InboundMessageFactoryCollection(
                             inboundConsumptionRecordFactories,
                             inboundMasterDataFactories,
                             inboundCMNotificationFactories,
                             inboundCMRevokeFactories
                     )
        );
    }

    @Test
    void updateActiveFactory_updatesRevokeFactory() {
        // Given
        EdaCMRevokeInboundMessageFactory revokeFactory1 = mock(EdaCMRevokeInboundMessageFactory.class);
        when(revokeFactory1.isActive(any())).thenReturn(true).thenReturn(false);

        EdaCMRevokeInboundMessageFactory revokeFactory2 = mock(EdaCMRevokeInboundMessageFactory.class);
        when(revokeFactory2.isActive(any())).thenReturn(true);

        var collection = new InboundMessageFactoryCollection(
                List.of(simpleEdaConsumptionRecordFactory(true)),
                List.of(simpleEdaMasterFactory(true)),
                List.of(simpleEdaCMNotificationFactory(true)),
                List.of(revokeFactory1, revokeFactory2)
        );

        assertEquals(revokeFactory1, collection.activeCMRevokeFactory());

        // When
        collection.updateActiveFactories();

        // Then
        assertEquals(revokeFactory2, collection.activeCMRevokeFactory());
    }

    @Test
    void updateActiveFactory_updatesNotificationFactory() {
        // Given
        EdaCMNotificationInboundMessageFactory notificationFactory1 = mock(EdaCMNotificationInboundMessageFactory.class);
        when(notificationFactory1.isActive(any())).thenReturn(true).thenReturn(false);

        EdaCMNotificationInboundMessageFactory notificationFactory2 = mock(EdaCMNotificationInboundMessageFactory.class);
        when(notificationFactory2.isActive(any())).thenReturn(true);

        var collection = new InboundMessageFactoryCollection(
                List.of(simpleEdaConsumptionRecordFactory(true)),
                List.of(simpleEdaMasterFactory(true)),
                List.of(notificationFactory1, notificationFactory2),
                List.of(simpleEdaCMRevokeFactory(true))
        );

        assertEquals(notificationFactory1, collection.activeCMNotificationFactory());

        // When
        collection.updateActiveFactories();

        // Then
        assertEquals(notificationFactory2, collection.activeCMNotificationFactory());
    }

    @Test
    void updateActiveFactory_updatesMasterDataFactory() {
        // Given
        EdaMasterDataInboundMessageFactory masterDataFactory1 = mock(EdaMasterDataInboundMessageFactory.class);
        when(masterDataFactory1.isActive(any())).thenReturn(true).thenReturn(false);

        EdaMasterDataInboundMessageFactory masterDataFactory2 = mock(EdaMasterDataInboundMessageFactory.class);
        when(masterDataFactory2.isActive(any())).thenReturn(true);

        var collection = new InboundMessageFactoryCollection(
                List.of(simpleEdaConsumptionRecordFactory(true)),
                List.of(masterDataFactory1, masterDataFactory2),
                List.of(simpleEdaCMNotificationFactory(true)),
                List.of(simpleEdaCMRevokeFactory(true))
        );

        assertEquals(masterDataFactory1, collection.activeMasterDataFactory());

        // When
        collection.updateActiveFactories();

        // Then
        assertEquals(masterDataFactory2, collection.activeMasterDataFactory());
    }

    @Test
    void updateActiveFactory_updatesConsumptionRecordFactory() {
        // Given
        EdaConsumptionRecordInboundMessageFactory consumptionRecordFactory1 = mock(
                EdaConsumptionRecordInboundMessageFactory.class);
        when(consumptionRecordFactory1.isActive(any())).thenReturn(true).thenReturn(false);

        EdaConsumptionRecordInboundMessageFactory consumptionRecordFactory2 = mock(
                EdaConsumptionRecordInboundMessageFactory.class);
        when(consumptionRecordFactory2.isActive(any())).thenReturn(true);

        var collection = new InboundMessageFactoryCollection(
                List.of(consumptionRecordFactory1, consumptionRecordFactory2),
                List.of(simpleEdaMasterFactory(true)),
                List.of(simpleEdaCMNotificationFactory(true)),
                List.of(simpleEdaCMRevokeFactory(true))
        );

        assertEquals(consumptionRecordFactory1, collection.activeConsumptionRecordFactory());

        // When
        collection.updateActiveFactories();

        // Then
        assertEquals(consumptionRecordFactory2, collection.activeConsumptionRecordFactory());
    }

    @Test
    void updateActiveFactory_doesNotUpdateFactoriesIfNoActiveFactoryFound() {
        // Given
        EdaConsumptionRecordInboundMessageFactory consumptionRecordFactory1 = mock(
                EdaConsumptionRecordInboundMessageFactory.class);
        when(consumptionRecordFactory1.isActive(any())).thenReturn(true).thenReturn(false);
        EdaConsumptionRecordInboundMessageFactory consumptionRecordFactory2 = mock(
                EdaConsumptionRecordInboundMessageFactory.class);
        when(consumptionRecordFactory2.isActive(any())).thenReturn(false);

        EdaMasterDataInboundMessageFactory masterDataFactory1 = mock(EdaMasterDataInboundMessageFactory.class);
        when(masterDataFactory1.isActive(any())).thenReturn(true).thenReturn(false);
        EdaMasterDataInboundMessageFactory masterDataFactory2 = mock(EdaMasterDataInboundMessageFactory.class);
        when(masterDataFactory2.isActive(any())).thenReturn(false);

        EdaCMNotificationInboundMessageFactory notificationFactory1 = mock(EdaCMNotificationInboundMessageFactory.class);
        when(notificationFactory1.isActive(any())).thenReturn(true).thenReturn(false);
        EdaCMNotificationInboundMessageFactory notificationFactory2 = mock(EdaCMNotificationInboundMessageFactory.class);
        when(notificationFactory2.isActive(any())).thenReturn(false);

        EdaCMRevokeInboundMessageFactory revokeFactory1 = mock(EdaCMRevokeInboundMessageFactory.class);
        when(revokeFactory1.isActive(any())).thenReturn(true).thenReturn(false);
        EdaCMRevokeInboundMessageFactory revokeFactory2 = mock(EdaCMRevokeInboundMessageFactory.class);
        when(revokeFactory2.isActive(any())).thenReturn(false);


        var collection = new InboundMessageFactoryCollection(
                List.of(consumptionRecordFactory1, consumptionRecordFactory2),
                List.of(masterDataFactory1, masterDataFactory2),
                List.of(notificationFactory1, notificationFactory2),
                List.of(revokeFactory1, revokeFactory2)
        );

        assertAll(
                () -> assertEquals(consumptionRecordFactory1, collection.activeConsumptionRecordFactory()),
                () -> assertEquals(masterDataFactory1, collection.activeMasterDataFactory()),
                () -> assertEquals(notificationFactory1, collection.activeCMNotificationFactory()),
                () -> assertEquals(revokeFactory1, collection.activeCMRevokeFactory())
        );

        // When
        collection.updateActiveFactories();

        // Then
        assertAll(
                () -> assertEquals(consumptionRecordFactory1, collection.activeConsumptionRecordFactory()),
                () -> assertEquals(masterDataFactory1, collection.activeMasterDataFactory()),
                () -> assertEquals(notificationFactory1, collection.activeCMNotificationFactory()),
                () -> assertEquals(revokeFactory1, collection.activeCMRevokeFactory())
        );
    }

    @Test
    void updateActiveFactory_UpdatesAllFactories() {
        // Given
        EdaConsumptionRecordInboundMessageFactory consumptionRecordFactory1 = mock(
                EdaConsumptionRecordInboundMessageFactory.class);
        when(consumptionRecordFactory1.isActive(any())).thenReturn(true).thenReturn(false);
        EdaConsumptionRecordInboundMessageFactory consumptionRecordFactory2 = mock(
                EdaConsumptionRecordInboundMessageFactory.class);
        when(consumptionRecordFactory2.isActive(any())).thenReturn(true);

        EdaMasterDataInboundMessageFactory masterDataFactory1 = mock(EdaMasterDataInboundMessageFactory.class);
        when(masterDataFactory1.isActive(any())).thenReturn(true).thenReturn(false);
        EdaMasterDataInboundMessageFactory masterDataFactory2 = mock(EdaMasterDataInboundMessageFactory.class);
        when(masterDataFactory2.isActive(any())).thenReturn(true);

        EdaCMNotificationInboundMessageFactory notificationFactory1 = mock(EdaCMNotificationInboundMessageFactory.class);
        when(notificationFactory1.isActive(any())).thenReturn(true).thenReturn(false);
        EdaCMNotificationInboundMessageFactory notificationFactory2 = mock(EdaCMNotificationInboundMessageFactory.class);
        when(notificationFactory2.isActive(any())).thenReturn(true);

        EdaCMRevokeInboundMessageFactory revokeFactory1 = mock(EdaCMRevokeInboundMessageFactory.class);
        when(revokeFactory1.isActive(any())).thenReturn(true).thenReturn(false);
        EdaCMRevokeInboundMessageFactory revokeFactory2 = mock(EdaCMRevokeInboundMessageFactory.class);
        when(revokeFactory2.isActive(any())).thenReturn(true);


        var collection = new InboundMessageFactoryCollection(
                List.of(consumptionRecordFactory1, consumptionRecordFactory2),
                List.of(masterDataFactory1, masterDataFactory2),
                List.of(notificationFactory1, notificationFactory2),
                List.of(revokeFactory1, revokeFactory2)
        );

        assertAll(
                () -> assertEquals(consumptionRecordFactory1, collection.activeConsumptionRecordFactory()),
                () -> assertEquals(masterDataFactory1, collection.activeMasterDataFactory()),
                () -> assertEquals(notificationFactory1, collection.activeCMNotificationFactory()),
                () -> assertEquals(revokeFactory1, collection.activeCMRevokeFactory())
        );

        // When
        collection.updateActiveFactories();

        // Then
        assertAll(
                () -> assertEquals(consumptionRecordFactory2, collection.activeConsumptionRecordFactory()),
                () -> assertEquals(masterDataFactory2, collection.activeMasterDataFactory()),
                () -> assertEquals(notificationFactory2, collection.activeCMNotificationFactory()),
                () -> assertEquals(revokeFactory2, collection.activeCMRevokeFactory())
        );
    }
}
