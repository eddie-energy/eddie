// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_12;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import energy.eddie.regionconnector.simulation.providers.cim.v1_04.ValidatedHistoricalDataMarketDocumentProviderV104;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataMarketDocumentProviderV112Test {
    @Mock
    private DocumentStreams streams;
    @Mock
    private CommonInformationModelConfiguration config;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothSubscriptionOrdersShareMappingAndKeepRemainingSubscriberActive(boolean subscribeV112First) {
        var readings = Sinks.many().unicast().<SimulatedMeterReading>onBackpressureBuffer();
        var subscriptions = new AtomicInteger();
        when(streams.getSimulatedMeterReadingStream())
                .thenReturn(readings.asFlux().doOnSubscribe(ignored -> subscriptions.incrementAndGet()));
        when(config.eligiblePartyNationalCodingScheme()).thenReturn(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME);
        var provider = new ValidatedHistoricalDataMarketDocumentProviderV104(streams, config);
        var adapter = new ValidatedHistoricalDataMarketDocumentProviderV112(provider);
        var source = new RecordingSubscriber<VHDEnvelope>();
        var target = new RecordingSubscriber<Envelope>();

        try {
            if (subscribeV112First) {
                adapter.getValidatedHistoricalDataV112Stream().subscribe(target);
                provider.getValidatedHistoricalDataMarketDocumentsStream().subscribe(source);
            } else {
                provider.getValidatedHistoricalDataMarketDocumentsStream().subscribe(source);
                adapter.getValidatedHistoricalDataV112Stream().subscribe(target);
            }
            assertEquals(Sinks.EmitResult.OK, readings.tryEmitNext(reading()));
            assertEquals(1, subscriptions.get());
            assertEquals(1, source.values.size());
            assertEquals(1, target.values.size());
            assertEquals(source.values.getFirst().getMarketDocument().getMRID(),
                         target.values.getFirst().getVHDMarketDocument().getMRID());
            // This configuration lookup is performed inside the real 1.04 mapper once per document.
            verify(config, times(1)).eligiblePartyNationalCodingScheme();

            if (subscribeV112First) {
                target.dispose();
            } else {
                source.dispose();
            }
            assertEquals(Sinks.EmitResult.OK, readings.tryEmitNext(reading()));
            assertEquals(subscribeV112First ? 2 : 1, source.values.size());
            assertEquals(subscribeV112First ? 1 : 2, target.values.size());
            assertEquals(1, subscriptions.get());
            verify(config, times(2)).eligiblePartyNationalCodingScheme();
            assertEquals(Sinks.EmitResult.OK, readings.tryEmitComplete());
            assertTrue(subscribeV112First ? source.completed : target.completed);
        } finally {
            source.dispose();
            target.dispose();
        }
    }

    @Test
    void v112OnlySubscriberMapsInputAndCompletes() {
        var readings = Sinks.many().unicast().<SimulatedMeterReading>onBackpressureBuffer();
        var subscriptions = new AtomicInteger();
        when(streams.getSimulatedMeterReadingStream())
                .thenReturn(readings.asFlux().doOnSubscribe(ignored -> subscriptions.incrementAndGet()));
        when(config.eligiblePartyNationalCodingScheme()).thenReturn(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME);
        var provider = new ValidatedHistoricalDataMarketDocumentProviderV104(streams, config);
        var adapter = new ValidatedHistoricalDataMarketDocumentProviderV112(provider);

        assertEquals(0, subscriptions.get());
        StepVerifier.create(adapter.getValidatedHistoricalDataV112Stream())
                    .then(() -> assertEquals(Sinks.EmitResult.OK, readings.tryEmitNext(reading())))
                    .assertNext(document -> {
                        assertEquals("pid", document.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
                        assertEquals(1, document.getVHDMarketDocument().getTimeSeries().size());
                        assertEquals(10.25, document.getVHDMarketDocument().getTimeSeries().getFirst()
                                                   .getSeriesPeriods().getFirst().getPoints().getFirst()
                                                   .getEnergyQuantityQuantity().doubleValue());
                    })
                    .then(() -> assertEquals(Sinks.EmitResult.OK, readings.tryEmitComplete()))
                    .verifyComplete();
        assertEquals(1, subscriptions.get());
        verify(config, times(1)).eligiblePartyNationalCodingScheme();
    }

    private static SimulatedMeterReading reading() {
        return new SimulatedMeterReading("cid", "dnid", "pid", "mid", ZonedDateTime.parse("2026-01-01T00:00Z"),
                                         "PT15M", List.of(new Measurement(10.25, Measurement.MeasurementType.MEASURED)));
    }

    private static class RecordingSubscriber<T> extends BaseSubscriber<T> {
        private final List<T> values = new ArrayList<>();
        private boolean completed;

        @Override
        protected void hookOnNext(T value) {
            values.add(value);
        }

        @Override
        protected void hookOnComplete() {
            completed = true;
        }
    }
}
