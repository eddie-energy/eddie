// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider;

import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.EdaMasterData01p32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentifiableStreamsTest {
    private final ObjectProvider<UnaryOperator<IdentifiableConsumptionRecord>> provider = new ObjectProvider<>() {
        @SuppressWarnings("NullableProblems")
        @Override
        public Stream<UnaryOperator<IdentifiableConsumptionRecord>> stream() {
            return Stream.empty();
        }
    };
    @Mock
    private EdaAdapter edaAdapter;

    @Test
    void testConsumptionRecordStream_returnsStream() {
        // Given
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();
        when(edaAdapter.getConsumptionRecordStream()).thenReturn(testPublisher.flux());
        when(edaAdapter.getMasterDataStream()).thenReturn(Flux.empty());
        var streams = new IdentifiableStreams(provider, edaAdapter);
        var today = LocalDate.now(ZoneOffset.UTC);
        var consumptionRecord = new IdentifiableConsumptionRecord(new SimpleEdaConsumptionRecord(),
                                                                  List.of(),
                                                                  today,
                                                                  today);

        // When
        var res = streams.consumptionRecordStream();

        // Then
        StepVerifier.create(res)
                    .then(() -> testPublisher.emit(consumptionRecord))
                    .then(testPublisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testMasterDataStream_returnsStream() {
        // Given
        TestPublisher<IdentifiableMasterData> testPublisher = TestPublisher.create();
        when(edaAdapter.getMasterDataStream()).thenReturn(testPublisher.flux());
        when(edaAdapter.getConsumptionRecordStream()).thenReturn(Flux.empty());
        var streams = new IdentifiableStreams(provider, edaAdapter);
        var masterData = new IdentifiableMasterData(new EdaMasterData01p32(null),
                                                    new SimplePermissionRequest("pid", "cid", "dnid"));

        // When
        var res = streams.masterDataStream();

        // Then
        StepVerifier.create(res)
                    .then(() -> testPublisher.emit(masterData))
                    .then(testPublisher::complete)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}