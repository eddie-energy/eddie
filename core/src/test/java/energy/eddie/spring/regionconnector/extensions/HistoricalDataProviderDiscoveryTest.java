// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.core.services.MessageStreamHub;
import energy.eddie.core.services.SupportedFeatureService;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.cds.providers.vhd.v1_04.CdsValidatedHistoricalDataMarketDocumentV104Provider;
import energy.eddie.regionconnector.cds.providers.vhd.v1_12.CdsValidatedHistoricalDataMarketDocumentV112Provider;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import energy.eddie.regionconnector.simulation.providers.cim.v1_04.ValidatedHistoricalDataMarketDocumentProviderV104;
import energy.eddie.regionconnector.simulation.providers.cim.v1_12.ValidatedHistoricalDataMarketDocumentProviderV112;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricalDataProviderDiscoveryTest {
    @Mock
    private MessageStreamHub hub;
    @Mock
    private RegionConnectorMetadata metadata;
    @Mock
    private CommonInformationModelConfiguration config;
    @Mock
    private IdentifiableStreams edaStreams;
    @Mock
    private energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams fluviusStreams;
    @Mock
    private FluviusOAuthConfiguration fluviusConfig;
    @Mock
    private energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams cdsStreams;
    @Mock
    private ValidatedHistoricalDataStream etaStream;
    @Mock
    private DeEtaPlusConfiguration etaConfig;
    @Mock
    private energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams energinetStreams;
    @Mock
    private energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams datadisStreams;
    @Mock
    private DatadisConfiguration datadisConfig;
    @Mock
    private EnergyDataService fingridService;
    @Mock
    private energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams enedisStreams;
    @Mock
    private EnedisConfiguration enedisConfig;
    @Mock
    private DocumentStreams simulationStreams;

    @ParameterizedTest
    @MethodSource("providers")
    void discoversConcreteVersionedProvidersAndReportsCapabilities(Class<?> sourceType, Class<?> adapterType) throws Exception {
        try (var context = context()) {
            context.register(sourceType);
            context.refresh();
            var extension = new RegionConnectorSupportedFeatureExtension(new SupportedFeatureService(), metadata);
            extension.setApplicationContext(context);
            assertTrue(extension.supportsValidatedHistoricalDataMarketDocumentsV1_04());
            assertFalse(extension.supportsValidatedHistoricalDataMarketDocumentsV1_12());
            assertTrue(Arrays.stream(sourceType.getDeclaredMethods())
                             .map(method -> method.getAnnotation(MessageStream.class))
                             .filter(annotation -> annotation != null)
                             .noneMatch(annotation -> annotation.value().equals(Envelope.class)));

            context.register(adapterType);
            var adapter = context.getBean(adapterType);
            var method = adapterType.getDeclaredMethod("getValidatedHistoricalDataV112Stream");
            assertEquals(Envelope.class, method.getAnnotation(MessageStream.class).value());
            assertInstanceOf(Flux.class, method.invoke(adapter));
            assertTrue(extension.supportsValidatedHistoricalDataMarketDocumentsV1_04());
            assertTrue(extension.supportsValidatedHistoricalDataMarketDocumentsV1_12());

            var registrar = new StreamProviderAndSupplierRegistrar();
            registrar.setApplicationContext(context);
            registrar.afterSingletonsInstantiated();
            verify(hub).registerProvider(eq(VHDEnvelope.class), any());
            verify(hub).registerProvider(eq(Envelope.class), any());
        }
    }

    @Test
    void allVersionedProvidersHaveDistinctBeanNamesAndInjectTheirConcreteSource() {
        try (var context = context()) {
            context.setAllowBeanDefinitionOverriding(false);
            providers().forEach(pair -> {
                var types = pair.get();
                context.register((Class<?>) types[0], (Class<?>) types[1]);
            });
            context.refresh();
            providers().forEach(pair -> {
                for (var type : pair.get()) {
                    assertEquals(1, context.getBeanNamesForType((Class<?>) type).length);
                }
            });
        }
    }

    private AnnotationConfigApplicationContext context() {
        // No provider acquisition or mapping is needed to discover the concrete stream declarations.
        lenient().when(edaStreams.consumptionRecordStream()).thenReturn(Flux.never());
        lenient().when(fluviusStreams.getMeteringData()).thenReturn(Flux.never());
        lenient().when(cdsStreams.validatedHistoricalData()).thenReturn(Flux.never());
        lenient().when(etaStream.validatedHistoricalData()).thenReturn(Flux.never());
        lenient().when(energinetStreams.getValidatedHistoricalDataStream()).thenReturn(Flux.never());
        lenient().when(datadisStreams.getValidatedHistoricalData()).thenReturn(Flux.never());
        lenient().when(fingridService.getIdentifiableValidatedHistoricalDataStream()).thenReturn(Flux.never());
        lenient().when(enedisStreams.getValidatedHistoricalData()).thenReturn(Flux.never());
        lenient().when(simulationStreams.getSimulatedMeterReadingStream()).thenReturn(Flux.never());
        var context = new AnnotationConfigApplicationContext();
        context.registerBean("hub", MessageStreamHub.class, () -> hub);
        context.registerBean("config", CommonInformationModelConfiguration.class, () -> config);
        context.registerBean("edaStreams", IdentifiableStreams.class, () -> edaStreams);
        context.registerBean("fluviusStreams", energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams.class, () -> fluviusStreams);
        context.registerBean("fluviusConfig", FluviusOAuthConfiguration.class, () -> fluviusConfig);
        context.registerBean("cdsStreams", energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams.class, () -> cdsStreams);
        context.registerBean("etaStream", ValidatedHistoricalDataStream.class, () -> etaStream);
        context.registerBean("etaConfig", DeEtaPlusConfiguration.class, () -> etaConfig);
        context.registerBean("energinetStreams", energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams.class, () -> energinetStreams);
        context.registerBean("datadisStreams", energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams.class, () -> datadisStreams);
        context.registerBean("datadisConfig", DatadisConfiguration.class, () -> datadisConfig);
        context.registerBean("fingridService", EnergyDataService.class, () -> fingridService);
        context.registerBean("enedisStreams", energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams.class, () -> enedisStreams);
        context.registerBean("enedisConfig", EnedisConfiguration.class, () -> enedisConfig);
        context.registerBean("simulationStreams", DocumentStreams.class, () -> simulationStreams);
        return context;
    }

    private static Stream<Arguments> providers() {
        return Stream.of(
                Arguments.of(
                        energy.eddie.regionconnector.at.eda.provider.v1_04.EdaValidatedHistoricalDataMarketDocumentProvider.class,
                        energy.eddie.regionconnector.at.eda.provider.v1_12.EdaValidatedHistoricalDataMarketDocumentProvider.class
                ),
                Arguments.of(
                        CdsValidatedHistoricalDataMarketDocumentV104Provider.class,
                        CdsValidatedHistoricalDataMarketDocumentV112Provider.class
                ),
                Arguments.of(
                        ValidatedHistoricalDataMarketDocumentProviderV104.class,
                        ValidatedHistoricalDataMarketDocumentProviderV112.class
                ),
                Arguments.of(
                        energy.eddie.regionconnector.de.eta.providers.v1_04.DeValidatedHistoricalDataMarketDocumentProvider.class,
                        energy.eddie.regionconnector.de.eta.providers.v1_12.DeValidatedHistoricalDataMarketDocumentProvider.class
                ),
                Arguments.of(
                        energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04.CimStreams.class,
                        energy.eddie.regionconnector.fi.fingrid.services.cim.v1_12.CimStreams.class
                ),
                Arguments.of(
                        energy.eddie.regionconnector.es.datadis.providers.v1_04.DatadisValidatedHistoricalDataMarketDocumentProvider.class,
                        energy.eddie.regionconnector.es.datadis.providers.v1_12.DatadisValidatedHistoricalDataMarketDocumentProvider.class
                ),
                Arguments.of(
                        energy.eddie.regionconnector.fr.enedis.providers.v1_04.EnedisValidatedHistoricalDataMarketDocumentProvider.class,
                        energy.eddie.regionconnector.fr.enedis.providers.v1_12.EnedisValidatedHistoricalDataMarketDocumentProvider.class
                ),
                Arguments.of(
                        energy.eddie.regionconnector.be.fluvius.provider.v1_04.FluviusValidatedHistoricalDataMarketDocumentProvider.class,
                        energy.eddie.regionconnector.be.fluvius.provider.v1_12.FluviusValidatedHistoricalDataMarketDocumentProvider.class
                ),
                Arguments.of(
                        energy.eddie.regionconnector.dk.energinet.providers.v1_04.EnerginetValidatedHistoricalDataMarketDocumentProvider.class,
                        energy.eddie.regionconnector.dk.energinet.providers.v1_12.EnerginetValidatedHistoricalDataMarketDocumentProvider.class
                )
        );
    }
}
