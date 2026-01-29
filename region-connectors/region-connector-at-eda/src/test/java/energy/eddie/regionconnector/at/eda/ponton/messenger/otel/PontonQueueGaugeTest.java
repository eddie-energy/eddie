// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger.otel;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.FindMessagesClient;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.Messages;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PontonQueueGaugeTest {
    private final PontonXPAdapterConfiguration config = new PontonXPAdapterConfiguration(
            "adapter",
            "1.0",
            "https://localhost",
            8080,
            "https://localhost:8080/api",
            "/",
            "admin",
            "password"
    );
    @Mock
    private FindMessagesClient client;

    @Test
    void gauge_recordsMessageAmount() {
        // Given
        var reader = InMemoryMetricReader.create();
        var meterProvider = SdkMeterProvider.builder()
                                            .registerMetricReader(reader)
                                            .build();
        var otel = OpenTelemetrySdk.builder()
                                   .setMeterProvider(meterProvider)
                                   .build();
        when(client.findMessages(any()))
                .thenReturn(Mono.just(new Messages(10, List.of())));
        new PontonQueueGauge(config, otel, client);

        // When
        var res = reader.collectAllMetrics();

        // Then
        OpenTelemetryAssertions.assertThat(res)
                .singleElement()
                .extracting(metric -> metric.getLongGaugeData().getPoints())
                .asInstanceOf(InstanceOfAssertFactories.collection(LongPointData.class))
                .singleElement()
                .extracting(LongPointData::getValue)
                .isEqualTo(10L);
    }
    @Test
    void gauge_recordsNothing_whenClientThrows() {
        // Given
        var reader = InMemoryMetricReader.create();
        var meterProvider = SdkMeterProvider.builder()
                                            .registerMetricReader(reader)
                                            .build();
        var otel = OpenTelemetrySdk.builder()
                                   .setMeterProvider(meterProvider)
                                   .build();
        when(client.findMessages(any()))
                .thenReturn(Mono.error(new RuntimeException()));
        new PontonQueueGauge(config, otel, client);

        // When
        var res = reader.collectAllMetrics();

        // Then
        OpenTelemetryAssertions.assertThat(res)
                               .isEmpty();
    }
}