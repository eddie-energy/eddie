// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers.cim.v1_12;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import energy.eddie.regionconnector.simulation.providers.HistoricalDataInput;
import energy.eddie.regionconnector.simulation.providers.cim.v1_04.ValidatedHistoricalDataMarketDocumentProviderV104;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.BaseSubscriber;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataMarketDocumentProviderV112Test {
    private final CommonInformationModelConfiguration config = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "ep");
    @Mock
    private CommonInformationModelConfiguration failingConfig;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothSubscriptionOrdersShareContextAndKeepRemainingSubscriberActive(boolean subscribeV112First) {
        try (var streams = new DocumentStreams(config, new ObjectMapper())) {
            var legacy = new ValidatedHistoricalDataMarketDocumentProviderV104(streams, config);
            var direct = new ValidatedHistoricalDataMarketDocumentProviderV112(streams, config);
            var source = new RecordingSubscriber<VHDEnvelope>();
            var target = new RecordingSubscriber<Envelope>();
            try {
                if (subscribeV112First) {
                    direct.getValidatedHistoricalDataV112Stream().subscribe(target);
                    legacy.getValidatedHistoricalDataMarketDocumentsStream().subscribe(source);
                } else {
                    legacy.getValidatedHistoricalDataMarketDocumentsStream().subscribe(source);
                    direct.getValidatedHistoricalDataV112Stream().subscribe(target);
                }
                streams.publish(reading());
                assertEquals(1, source.values.size());
                assertEquals(1, target.values.size());
                assertCorrelated(source.values.getFirst(), target.values.getFirst());
                if (subscribeV112First) {
                    target.dispose();
                } else {
                    source.dispose();
                }
                streams.publish(reading());
                assertEquals(subscribeV112First ? 2 : 1, source.values.size());
                assertEquals(subscribeV112First ? 1 : 2, target.values.size());
                streams.close();
                assertTrue(subscribeV112First ? source.completed : target.completed);
            } finally {
                source.dispose();
                target.dispose();
            }
        }
    }

    @Test
    void v112OnlyMapsPopulatedRawInputAndCompletes() {
        try (var streams = new DocumentStreams(config, new ObjectMapper())) {
            var direct = new ValidatedHistoricalDataMarketDocumentProviderV112(streams, config);
            StepVerifier.create(direct.getValidatedHistoricalDataV112Stream())
                        .then(() -> streams.publish(reading()))
                        .assertNext(document -> {
                            assertEquals("pid", document.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
                            assertEquals(1, document.getVHDMarketDocument().getTimeSeries().size());
                            var series = document.getVHDMarketDocument().getTimeSeries().getFirst();
                            assertEquals(1, series.getSeriesPeriods().size());
                            assertEquals(1, series.getSeriesPeriods().getFirst().getPoints().size());
                            assertEquals(10.25, series.getSeriesPeriods().getFirst().getPoints().getFirst()
                                                      .getEnergyQuantityQuantity().doubleValue());
                        })
                        .then(streams::close)
                        .verifyComplete();
        }
    }

    @Test
    void v104OnlyMapsWithoutDirectProvider() {
        try (var streams = new DocumentStreams(config, new ObjectMapper())) {
            var legacy = new ValidatedHistoricalDataMarketDocumentProviderV104(streams, config);
            StepVerifier.create(legacy.getValidatedHistoricalDataMarketDocumentsStream())
                        .then(() -> streams.publish(reading()))
                        .assertNext(document -> {
                            assertEquals("pid", document.getMessageDocumentHeaderMetaInformationPermissionId());
                            assertEquals(1, document.getMarketDocument().getTimeSeries().size());
                            assertEquals(10.25, document.getMarketDocument().getTimeSeries().getFirst()
                                                       .getPeriods().getFirst().getPoints().getFirst()
                                                       .getEnergyQuantityQuantity().doubleValue());
                        })
                        .then(streams::close)
                        .verifyComplete();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mappingFailureIsLocalAndLaterValidInputStillMaps(boolean failLegacy) {
        when(failingConfig.eligiblePartyNationalCodingScheme())
                .thenThrow(new IllegalArgumentException("invalid configuration"))
                .thenReturn(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME);
        try (var streams = new DocumentStreams(config, new ObjectMapper())) {
            var legacy = new ValidatedHistoricalDataMarketDocumentProviderV104(streams, failLegacy ? failingConfig : config);
            var direct = new ValidatedHistoricalDataMarketDocumentProviderV112(streams, failLegacy ? config : failingConfig);
            var source = new RecordingSubscriber<VHDEnvelope>();
            var target = new RecordingSubscriber<Envelope>();
            try {
                legacy.getValidatedHistoricalDataMarketDocumentsStream().subscribe(source);
                direct.getValidatedHistoricalDataV112Stream().subscribe(target);
                streams.publish(reading());
                assertEquals(failLegacy ? 0 : 1, source.values.size());
                assertEquals(failLegacy ? 1 : 0, target.values.size());
                streams.publish(reading());
                assertEquals(failLegacy ? 1 : 2, source.values.size());
                assertEquals(failLegacy ? 2 : 1, target.values.size());
                assertCorrelated(source.values.getLast(), target.values.getLast());
                streams.close();
                assertTrue(source.completed);
                assertTrue(target.completed);
            } finally {
                source.dispose();
                target.dispose();
            }
        }
    }

    @Test
    void preparedInputRetainsOriginalAndAllocatesMetadataPerEmission() {
        var reading = reading();
        try (var streams = new DocumentStreams(config, new ObjectMapper())) {
            var inputs = new RecordingSubscriber<HistoricalDataInput>();
            var documents = new RecordingSubscriber<Envelope>();
            var provider = new ValidatedHistoricalDataMarketDocumentProviderV112(streams, config);
            try {
                streams.historicalDataInputs().subscribe(inputs);
                provider.getValidatedHistoricalDataV112Stream().subscribe(documents);
                streams.publish(reading);
                streams.publish(reading);
                assertEquals(2, inputs.values.size());
                assertEquals(2, documents.values.size());
                assertSame(reading, inputs.values.getFirst().source());
                assertSame(reading, inputs.values.getLast().source());
                assertNotSame(inputs.values.getFirst().metadata(), inputs.values.getLast().metadata());
                assertNotEquals(documents.values.getFirst().getVHDMarketDocument().getMRID(),
                                documents.values.getLast().getVHDMarketDocument().getMRID());
                assertEquals(inputs.values.getFirst().metadata().id("document"),
                             documents.values.getFirst().getVHDMarketDocument().getMRID());
            } finally {
                inputs.dispose();
                documents.dispose();
            }
        }
    }

    private static void assertCorrelated(VHDEnvelope source, Envelope target) {
        assertEquals(source.getMarketDocument().getMRID(), target.getVHDMarketDocument().getMRID());
        assertEquals(source.getMarketDocument().getCreatedDateTime(), target.getVHDMarketDocument().getCreatedDateTime());
        assertEquals(source.getMessageDocumentHeaderCreationDateTime(), target.getMessageDocumentHeader().getCreationDateTime());
        assertEquals(source.getMarketDocument().getTimeSeries().getFirst().getMRID(),
                     target.getVHDMarketDocument().getTimeSeries().getFirst().getMRID());
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
